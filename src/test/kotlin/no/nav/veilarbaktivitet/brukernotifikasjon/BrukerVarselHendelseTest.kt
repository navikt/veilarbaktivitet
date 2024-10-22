package no.nav.veilarbaktivitet.brukernotifikasjon

import com.github.tomakehurst.wiremock.client.WireMock
import io.micrometer.core.instrument.MeterRegistry
import lombok.SneakyThrows
import no.nav.common.json.JsonUtils
import no.nav.tms.varsel.action.Varseltype
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.VarselKvitteringStatus
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.AktivitetVarsel
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendMinsideVarselFraOutboxCron
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.VarselDAO
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselHendelseDTO
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselKanal
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselStatus
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.VarselEventTypeDto
import no.nav.veilarbaktivitet.db.DbTestUtils
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.mock_nav_modell.NavMockService
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import no.nav.veilarbaktivitet.util.KafkaTestService
import org.apache.kafka.clients.consumer.Consumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.util.*

internal class BrukerVarselHendelseTest(
    @Autowired
    val minsideVarselService: MinsideVarselService,
    @Autowired
    val avsluttBrukernotifikasjonCron: AvsluttBrukernotifikasjonCron,
    @Autowired
    val sendMinsideVarselFraOutboxCron: SendMinsideVarselFraOutboxCron,
    @Autowired
    val kafkaTestService: KafkaTestService,
    @Autowired
    val brukernotifikasjonAssertsConfig: BrukernotifikasjonAssertsConfig,
    @Autowired
    val varselDao: VarselDAO,
    @Value("\${topic.ut.brukernotifikasjon.brukervarsel}")
    val brukervarselTopic: String,
    @Autowired
    val jdbc: NamedParameterJdbcTemplate,
    @Autowired
    val meterRegistry: MeterRegistry,
    @Value("\${app.env.aktivitetsplan.basepath}")
    val basepath: String,
    @Autowired
    val navMockService: NavMockService
) : SpringBootTestBase() {

    lateinit var brukerVarselConsumer: Consumer<String, String>
    lateinit var brukernotifikasjonAsserts: BrukernotifikasjonAsserts
    @BeforeEach
    fun setUp() {
        DbTestUtils.cleanupTestDb(jdbc.jdbcTemplate)
        brukerVarselConsumer = kafkaTestService.createStringStringConsumer(brukervarselTopic)
        brukernotifikasjonAsserts = BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig!!)
    }

    @AfterEach
    fun assertNoUnkowns() {
        brukerVarselConsumer.unsubscribe()

        Assertions.assertTrue(WireMock.findUnmatchedRequests().isEmpty())
    }

    @Test
    fun skalIkkeBehandleKvitteringerFraAndreApper() {
        // opprett oppgave
        // send kvittering med samme varselid, men annen app
        // sjekk status ikke endret
        // send kvittering for vår app
        // sjekk status endret
    }

    @SneakyThrows
    @Test
    fun notifikasjonsstatus_tester() {
        val mockBruker = navMockService.createHappyBruker()
        val aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet()
        val skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false)
        val aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes)


        val oppgaveRecord = opprettOppgave(mockBruker, aktivitetDTO)
        val eventId = UUID.fromString(oppgaveRecord.varselId)

        assertVarselStatus(eventId, VarselStatus.SENDT)

        assertEksternVarselKvitteringStatus(eventId, VarselKvitteringStatus.IKKE_SATT)

        brukernotifikasjonAsserts.simulerEksternVarselStatusSendt(oppgaveRecord)

        assertEksternVarselKvitteringStatus(eventId, VarselKvitteringStatus.OK)

        brukernotifikasjonAsserts.simulerEksternVarselStatusFeilet(oppgaveRecord)

        assertEksternVarselKvitteringStatus(eventId, VarselKvitteringStatus.FEILET)

        brukernotifikasjonAsserts.simulerEksternVarselStatusSendt(oppgaveRecord)

        assertEksternVarselKvitteringStatus(eventId, VarselKvitteringStatus.OK)

        sendMinsideVarselFraOutboxCron.countForsinkedeVarslerSisteDognet()
        val gauge = meterRegistry.find("brukernotifikasjon_mangler_kvittering").gauge()
        Assertions.assertEquals(0.0, gauge?.value())
    }

    @SneakyThrows
    @Test
    fun ekstern_done_event() {
        val mockBruker = navMockService.createHappyBruker()
        val aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet()
        val skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false)
        val aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes)
        Assertions.assertEquals(0, varselDao.hentAntallUkvitterteVarslerForsoktSendt(-1))

        val oppgaveVarsel = opprettOppgave(mockBruker, aktivitetDTO)
        val varselId = UUID.fromString(oppgaveVarsel.varselId)

        assertVarselStatus(varselId, VarselStatus.SENDT)

        assertEksternVarselKvitteringStatus(varselId, VarselKvitteringStatus.IKKE_SATT)
        brukernotifikasjonAsserts.simulerEksternVarselStatusSendt(oppgaveVarsel)

        assertEksternVarselKvitteringStatus(varselId, VarselKvitteringStatus.OK)
    }


    private fun assertVarselStatus(varselId: UUID, varselStatus: VarselStatus) {
        val param = MapSqlParameterSource()
            .addValue("eventId", varselId.toString())
        val status = jdbc.queryForObject(
            "SELECT STATUS from BRUKERNOTIFIKASJON where BRUKERNOTIFIKASJON_ID = :eventId", param,
            String::class.java
        ) //TODO fiks denne når vi eksponerer det ut til apiet
        Assertions.assertEquals(varselStatus.name, status)
    }

    private fun assertEksternVarselKvitteringStatus(eventId: UUID, expectedVarselStatus: VarselKvitteringStatus) {
        val param = MapSqlParameterSource()
            .addValue("eventId", eventId.toString())
        val status = jdbc.queryForObject(
            "SELECT VARSEL_KVITTERING_STATUS from BRUKERNOTIFIKASJON where BRUKERNOTIFIKASJON_ID = :eventId", param,
            String::class.java
        ) //TODO fiks denne når vi eksponerer det ut til apiet
        Assertions.assertEquals(expectedVarselStatus.name, status)
    }

    private fun status(varselId: UUID, status: EksternVarselStatus): EksternVarselHendelseDTO {
        return EksternVarselHendelseDTO(
            namespace = "dab",
            appnavn = "veilarbaktivitet",
            varseltype = Varseltype.Oppgave,
            eventName = VarselEventTypeDto.eksternStatusOppdatert.name,
            varselId = varselId,
            status = status,
            renotifikasjon = false,
            feilmelding = null,
            kanal = EksternVarselKanal.SMS,
        )
    }

    private fun bestiltStatus(bestillingsId: UUID): EksternVarselHendelseDTO {
        return status(bestillingsId, EksternVarselStatus.sendt)
    }

    private fun sendtStatus(bestillingsId: UUID): EksternVarselHendelseDTO {
        return status(bestillingsId, EksternVarselStatus.sendt)
    }

    private fun feiletStatus(bestillingsId: UUID): EksternVarselHendelseDTO {
        return status(bestillingsId, EksternVarselStatus.feilet)
    }

//    private fun infoStatus(bestillingsId: UUID): VarselHendelse {
//        return status(bestillingsId, INFO)
//    }

//    private fun oversendtStatus(eventId: String): VarselHendelse {
//        return status(eventId, OVERSENDT)
//    }

    private fun opprettOppgave(mockBruker: MockBruker, aktivitetDTO: AktivitetDTO): OpprettVarselDto {
        minsideVarselService.opprettVarselPaaAktivitet(
            AktivitetVarsel(
                aktivitetDTO.id.toLong(),
                aktivitetDTO.versjon.toLong(),
                mockBruker.aktorId,
                "Testvarsel",
                VarselType.STILLING_FRA_NAV, null, null, null
            )
        )

        sendMinsideVarselFraOutboxCron.sendBrukernotifikasjoner()
        avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner()

        //        assertTrue(kafkaTestService.harKonsumertAlleMeldinger(doneTopic, doneConsumer), "Skal ikke produsert done meldinger");
        val oppgaveRecord = KafkaTestUtils.getSingleRecord(
            brukerVarselConsumer,
            brukervarselTopic,
            KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION
        )
        val oppgave = JsonUtils.fromJson(
            oppgaveRecord.value(),
            OpprettVarselDto::class.java
        )

        //        assertEquals(mockBruker.getOppfolgingsperiodeId().toString(), nokkel.getGrupperingsId());
        Assertions.assertEquals(mockBruker.fnr, oppgave.ident)
        Assertions.assertEquals(basepath + "/aktivitet/vis/" + aktivitetDTO.id, oppgave.link)
        return oppgave
    }

    companion object {
        private const val OPPGAVE_KVITERINGS_PREFIX = "O-veilarbaktivitet-"
    }
}
