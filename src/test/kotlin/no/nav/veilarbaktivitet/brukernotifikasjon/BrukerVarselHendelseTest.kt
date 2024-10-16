package no.nav.veilarbaktivitet.brukernotifikasjon

import com.github.tomakehurst.wiremock.client.WireMock
import io.micrometer.core.instrument.MeterRegistry
import lombok.SneakyThrows
import no.nav.common.json.JsonUtils
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus
import no.nav.tms.varsel.action.Varseltype
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.brukernotifikasjon.avslutt.AvsluttBrukernotifikasjonCron
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.VarselKvitteringStatus
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.AktivitetVarsel
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.VarselDAO
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.*
import no.nav.veilarbaktivitet.db.DbTestUtils
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import no.nav.veilarbaktivitet.util.KafkaTestService
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
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
    var brukernotifikasjonService: BrukernotifikasjonService,
    @Autowired
    var avsluttBrukernotifikasjonCron: AvsluttBrukernotifikasjonCron,
    @Autowired
    var sendBrukernotifikasjonCron: SendBrukernotifikasjonCron,
    @Autowired
    var kafkaTestService: KafkaTestService,
    @Autowired
    var brukernotifikasjonAssertsConfig: BrukernotifikasjonAssertsConfig,
    @Autowired
    var varselDao: VarselDAO,
    @Value("\${topic.ut.brukernotifikasjon.brukervarsel}")
    var brukervarselTopic: String,
    var brukerVarselConsumer: Consumer<String, String>,
    @Autowired
    var jdbc: NamedParameterJdbcTemplate,
    @Autowired
    var eksternVarslingKvitteringConsumer: VarselHendelseConsumer,
    @Autowired
    var meterRegistry: MeterRegistry,
    @Value("\${app.env.aktivitetsplan.basepath}")
    var basepath: String,
    var brukernotifikasjonAsserts: BrukernotifikasjonAsserts,
) : SpringBootTestBase() {


    @BeforeEach
    fun setUp() {
        DbTestUtils.cleanupTestDb(jdbc!!.jdbcTemplate)
        brukerVarselConsumer = kafkaTestService.createStringStringConsumer(brukervarselTopic)
        brukernotifikasjonAsserts = BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig!!)
    }

    @AfterEach
    fun assertNoUnkowns() {
        brukerVarselConsumer!!.unsubscribe()

        Assertions.assertTrue(WireMock.findUnmatchedRequests().isEmpty())
    }

    @SneakyThrows
    @Test
    fun notifikasjonsstatus_tester() {
        val mockBruker = MockNavService.createHappyBruker()
        val aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet()
        val skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false)
        val aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes)
        Assertions.assertEquals(0, varselDao!!.hentAntallUkvitterteVarslerForsoktSendt(-1))


        val oppgaveRecord = opprettOppgave(mockBruker, aktivitetDTO)
        val eventId = UUID.fromString(oppgaveRecord.varselId)

        assertVarselStatusErSendt(eventId)
        Assertions.assertEquals(1, varselDao!!.hentAntallUkvitterteVarslerForsoktSendt(-1))

        assertEksternVarselStatus(eventId, VarselKvitteringStatus.IKKE_SATT)

        skalIkkeBehandleMedAnnenBestillingsId(eventId)

        infoOgOVersendtSkalIkkeEndreStatus(eventId, VarselKvitteringStatus.IKKE_SATT)

        consumAndAssertStatus(eventId, okStatus(eventId), VarselKvitteringStatus.OK)

        Assertions.assertEquals(0, varselDao!!.hentAntallUkvitterteVarslerForsoktSendt(-1))

        infoOgOVersendtSkalIkkeEndreStatus(eventId, VarselKvitteringStatus.OK)

        consumAndAssertStatus(eventId, feiletStatus(eventId), VarselKvitteringStatus.FEILET)
        consumAndAssertStatus(eventId, okStatus(eventId), VarselKvitteringStatus.FEILET)

        infoOgOVersendtSkalIkkeEndreStatus(eventId, VarselKvitteringStatus.FEILET)

        sendBrukernotifikasjonCron!!.countForsinkedeVarslerSisteDognet()
        val gauge = meterRegistry!!.find("brukernotifikasjon_mangler_kvittering").gauge()
        Assertions.assertEquals(0.0, gauge!!.value())


        val brukernotifikasjonId = OPPGAVE_KVITERINGS_PREFIX + eventId
        val ugyldigstatus =
            ConsumerRecord("VarselKviteringToppic", 1, 1, brukernotifikasjonId, status(eventId, "ugyldig_status"))
        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { eksternVarslingKvitteringConsumer!!.consume(ugyldigstatus) }

        val feilprefixId = "feilprefix-$eventId"

        val melding = DoknotifikasjonStatus
            .newBuilder()
            .setStatus(OVERSENDT)
            .setBestillingsId(feilprefixId)
            .setBestillerId("veilarbaktivitet")
            .setMelding("her er en melding")
            .setDistribusjonId(1L)
            .build()
        val feilPrefix = ConsumerRecord("VarselKviteringToppic", 1, 1, feilprefixId, melding)
        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { eksternVarslingKvitteringConsumer!!.consume(feilPrefix) }

        assertVarselStatusErSendt(eventId) //SKAl ikke ha endret seg
        assertEksternVarselStatus(eventId, VarselKvitteringStatus.FEILET) //SKAl ikke ha endret seg
    }

    @SneakyThrows
    @Test
    fun ekstern_done_event() {
        val mockBruker = MockNavService.createHappyBruker()
        val aktivitetData = AktivitetDataTestBuilder.nyEgenaktivitet()
        val skalOpprettes = AktivitetDTOMapper.mapTilAktivitetDTO(aktivitetData, false)
        val aktivitetDTO = aktivitetTestService.opprettAktivitet(mockBruker, skalOpprettes)
        Assertions.assertEquals(0, varselDao!!.hentAntallUkvitterteVarslerForsoktSendt(-1))


        val oppgave = opprettOppgave(mockBruker, aktivitetDTO)
        val varselId = UUID.fromString(oppgave.varselId)

        assertVarselStatusErSendt(varselId)
        Assertions.assertEquals(1, varselDao!!.hentAntallUkvitterteVarslerForsoktSendt(-1))

        assertEksternVarselStatus(varselId, VarselKvitteringStatus.IKKE_SATT)
        brukernotifikasjonAsserts!!.simulerEksternVarselSendt(oppgave)

        assertVarselStatusErAvsluttet(varselId)
        assertEksternVarselStatus(varselId, VarselKvitteringStatus.IKKE_SATT)
    }

    private fun infoOgOVersendtSkalIkkeEndreStatus(
        eventId: UUID,
        expectedVarselKvitteringStatus: VarselKvitteringStatus
    ) {
        consumAndAssertStatus(eventId, bestiltStatus(eventId), expectedVarselKvitteringStatus)
    }

    private fun skalIkkeBehandleMedAnnenBestillingsId(eventId: UUID) {
        val statusMedAnnenBestillerId = bestiltStatus(eventId)
        statusMedAnnenBestillerId.bestillerId = "annen_bestillerid"

        consumAndAssertStatus(eventId, statusMedAnnenBestillerId, VarselKvitteringStatus.IKKE_SATT)
    }


    private fun consumAndAssertStatus(
        varselId: UUID,
        message: EksternVarselHendelseDTO,
        expectedEksternVarselStatus: VarselKvitteringStatus
    ) {
        eksternVarslingKvitteringConsumer.consume(
            ConsumerRecord<String?, String>(
                "VarselKviteringToppic",
                1,
                1,
                varselId.toString(),
                JsonUtils.toJson(message)
            )
        )

        assertVarselStatusErSendt(varselId)
        assertEksternVarselStatus(varselId, expectedEksternVarselStatus)
    }

    private fun assertVarselStatusErSendt(eventId: UUID) {
        assertVarselStatus(eventId, VarselStatus.SENDT)
    }

    private fun assertVarselStatusErAvsluttet(eventId: UUID) {
        assertVarselStatus(eventId, VarselStatus.AVSLUTTET)
    }

    private fun assertVarselStatus(varselId: UUID, varselStatus: VarselStatus) {
        val param = MapSqlParameterSource()
            .addValue("eventId", varselId.toString())
        val status = jdbc!!.queryForObject(
            "SELECT STATUS from BRUKERNOTIFIKASJON where BRUKERNOTIFIKASJON_ID = :eventId", param,
            String::class.java
        ) //TODO fiks denne når vi eksponerer det ut til apiet
        Assertions.assertEquals(varselStatus.name, status)
    }

    private fun assertEksternVarselStatus(eventId: UUID, expectedVarselStatus: VarselKvitteringStatus) {
        val param = MapSqlParameterSource()
            .addValue("eventId", eventId.toString())
        val status = jdbc!!.queryForObject(
            "SELECT VARSEL_KVITTERING_STATUS from BRUKERNOTIFIKASJON where BRUKERNOTIFIKASJON_ID = :eventId", param,
            String::class.java
        ) //TODO fiks denne når vi eksponerer det ut til apiet
        Assertions.assertEquals(expectedVarselStatus.name, status)
    }

    private fun status(varselId: UUID, status: EksternVarselStatus): EksternVarselHendelseDTO {
        return EksternVarselHendelseDTO(
            namespace = "dab",
            appnavn = "veilarbaktivitet",
            varseltype = Varseltype.Oppgave.name,
            eventName = EksternVarselEventType.eksternStatusOppdatert.name,
            varselId = varselId,
            status = status,
            renotifikasjon = false,
            feilmelding = null,
            kanal = EksternVarselKanal.SMS,
        )
    }

    private fun bestiltStatus(bestillingsId: UUID): VarselHendelse {
        return status(bestillingsId, EksternVarselStatus.sendt)
    }

    private fun sendtStatus(bestillingsId: UUID): VarselHendelse {
        return status(bestillingsId, EksternVarselStatus.sendt)
    }

    private fun feiletStatus(bestillingsId: UUID): VarselHendelse {
        return status(bestillingsId, EksternVarselStatus.feilet)
    }

//    private fun infoStatus(bestillingsId: UUID): VarselHendelse {
//        return status(bestillingsId, INFO)
//    }

//    private fun oversendtStatus(eventId: String): VarselHendelse {
//        return status(eventId, OVERSENDT)
//    }

    private fun opprettOppgave(mockBruker: MockBruker, aktivitetDTO: AktivitetDTO): OpprettVarselDto {
        brukernotifikasjonService!!.opprettVarselPaaAktivitet(
            AktivitetVarsel(
                aktivitetDTO.id.toLong(),
                aktivitetDTO.versjon.toLong(),
                mockBruker.aktorId,
                "Testvarsel",
                VarselType.STILLING_FRA_NAV, null, null, null
            )
        )

        sendBrukernotifikasjonCron!!.sendBrukernotifikasjoner()
        avsluttBrukernotifikasjonCron!!.avsluttBrukernotifikasjoner()

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
