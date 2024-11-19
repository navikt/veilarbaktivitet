package no.nav.veilarbaktivitet.brukernotifikasjon

import com.github.tomakehurst.wiremock.client.WireMock
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.cumulative.CumulativeCounter
import lombok.SneakyThrows
import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.VarselHendelseMetrikk.VARSEL_HENDELSE
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.VarselKvitteringStatus
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.AktivitetVarsel
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideVarselId
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendMinsideVarselFraOutboxCron
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.VarselDAO
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.InternVarselHendelseType
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
        meterRegistry.find(VARSEL_HENDELSE).meters()
            .forEach { meterRegistry.remove(it) }
    }

    @AfterEach
    fun assertNoUnkowns() {
        brukerVarselConsumer.unsubscribe()

        Assertions.assertTrue(WireMock.findUnmatchedRequests().isEmpty())
    }

    @Test
    fun skalIkkeBehandleKvitteringerFraAndreApper() {
        /* Asserter at meldingen er konsummert inni utility funksjonen */
        brukernotifikasjonAsserts.simulerVarselFraAnnenApp()
    }

    @SneakyThrows
    @Test
    fun notifikasjonsstatus_tester() {
        val mockBruker = navMockService.createHappyBruker()
        val aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet()
        val skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false)
        val aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes)


        val oppgaveRecord = opprettVarsel(mockBruker, aktivitetDTO, VarselType.STILLING_FRA_NAV)
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

    @Test
    fun `ekstern-varsel-hendelse FERDIGSTILT skal sette kvitteringsstatus OK`() {
        val mockBruker = navMockService.createHappyBruker()
        val aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet()
        val skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false)
        val aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes)

        val beskjed = opprettVarsel(mockBruker, aktivitetDTO, VarselType.IKKE_FATT_JOBBEN)
        val eventId = UUID.fromString(beskjed.varselId)

        assertVarselStatus(eventId, VarselStatus.SENDT)

        brukernotifikasjonAsserts.simulerEksternVarselStatusFerdigstilt(beskjed)
        assertEksternVarselKvitteringStatus(eventId, VarselKvitteringStatus.OK)
    }

    @Test
    fun `ekstern-varsel-hendelse KANSELLERT skal sette kvitteringsstatus OK`() {
        val mockBruker = navMockService.createHappyBruker()
        val aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet()
        val skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false)
        val aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes)

        val beskjed = opprettVarsel(mockBruker, aktivitetDTO, VarselType.STILLING_FRA_NAV)
        val eventId = UUID.fromString(beskjed.varselId)

        assertVarselStatus(eventId, VarselStatus.SENDT)

        brukernotifikasjonAsserts.simulerEksternVarselStatusKansellert(beskjed)
        assertEksternVarselKvitteringStatus(eventId, VarselKvitteringStatus.OK)
    }

    @Test
    fun `varsel-hendelse inaktivert skal sette status AVSLUTTET`() {
        val mockBruker = navMockService.createHappyBruker()
        val aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet()
        val skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false)
        val aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes)

        val beskjed = opprettVarsel(mockBruker, aktivitetDTO, VarselType.STILLING_FRA_NAV)
        val varselId = UUID.fromString(beskjed.varselId)

        assertVarselStatus(varselId, VarselStatus.SENDT)

        brukernotifikasjonAsserts.simulerInternVarselStatusHendelse(
            MinSideVarselId(varselId),
            InternVarselHendelseType.inaktivert ,
            beskjed.type)
        assertVarselStatus(varselId, VarselStatus.AVSLUTTET)
    }

    @SneakyThrows
    @Test
    fun ekstern_done_event() {
        val mockBruker = navMockService.createHappyBruker()
        val aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet()
        val skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false)
        val aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes)
        Assertions.assertEquals(0, varselDao.hentAntallUkvitterteVarslerForsoktSendt(-1))

        val oppgaveVarsel = opprettVarsel(mockBruker, aktivitetDTO, VarselType.FORHAANDSORENTERING)
        val varselId = UUID.fromString(oppgaveVarsel.varselId)

        assertVarselStatus(varselId, VarselStatus.SENDT)

        assertEksternVarselKvitteringStatus(varselId, VarselKvitteringStatus.IKKE_SATT)
        brukernotifikasjonAsserts.simulerEksternVarselStatusSendt(oppgaveVarsel)

        assertEksternVarselKvitteringStatus(varselId, VarselKvitteringStatus.OK)

        val totalVarselHendelser = meterRegistry.find(VARSEL_HENDELSE)
            .counters()
            .filter { (it as CumulativeCounter).id.tags.any { tag -> tag.value == "sendt_ekstern" } }
            .sumOf { it.count() }
        Assertions.assertEquals(1.0, totalVarselHendelser)
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

    private fun opprettVarsel(mockBruker: MockBruker, aktivitetDTO: AktivitetDTO, varselType: VarselType): OpprettVarselDto {
        minsideVarselService.opprettVarselPaaAktivitet(
            AktivitetVarsel(
                aktivitetDTO.id.toLong(),
                aktivitetDTO.versjon.toLong(),
                mockBruker.aktorId,
                "Testvarsel",
                varselType, null, null, null
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
}
