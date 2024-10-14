package no.nav.veilarbaktivitet.brukernotifikasjon

import lombok.SneakyThrows
import no.nav.brukernotifikasjon.schemas.input.DoneInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.common.json.JsonUtils
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus
import no.nav.tms.varsel.action.Varseltype
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideBrukernotifikasjonsId
import no.nav.veilarbaktivitet.brukernotifikasjon.kvittering.EksternVarslingKvitteringConsumer
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.util.KafkaTestService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.util.*

class BrukernotifikasjonAsserts(var config: BrukernotifikasjonAssertsConfig) {
    var brukervarselConsumer = config.createBrukerVarselConsumer()
    var doneInputConsumer = config.createBrukernotifikasjonVarselHendelseConsumer()
    private val kviteringsProducer = config.kviteringsProducer

    var kafkaTestService: KafkaTestService = config.testService

    fun assertOppgaveSendt(fnr: Person.Fnr): VarselDto {
        config.sendBrukernotifikasjonCron.sendBrukernotifikasjoner()
        val singleRecord = KafkaTestUtils.getSingleRecord(
            brukervarselConsumer,
            config.brukernotifikasjonBrukervarselTopic,
            KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION
        )
        val value = JsonUtils.fromJson(singleRecord.value(), VarselDto::class.java)
        Assertions.assertEquals(fnr.get(), value.getIdent())
        Assertions.assertEquals(config.appname, value.getProdusent().getAppnavn())
        Assertions.assertEquals(config.namespace, value.getProdusent().getNamespace())
        return value
    }

    fun assertBeskjedSendt(fnr: Person.Fnr): VarselDto {
        config.sendBrukernotifikasjonCron.sendBrukernotifikasjoner()
        val singleRecord = KafkaTestUtils.getSingleRecord(
            brukervarselConsumer,
            config.brukernotifikasjonBrukervarselTopic,
            KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION
        )
        val varsel = JsonUtils.fromJson(singleRecord.value(), VarselDto::class.java)
        Assertions.assertEquals(fnr.get(), varsel.getIdent())
        Assertions.assertEquals(config.appname, varsel.getProdusent().getAppnavn())
        Assertions.assertEquals(config.namespace, varsel.getProdusent().getNamespace())
        return varsel
    }

    fun assertIngenNyeBeskjeder() {
        config.sendBrukernotifikasjonCron.sendBrukernotifikasjoner()
        org.assertj.core.api.Assertions.assertThat(
            kafkaTestService.harKonsumertAlleMeldinger(
                config.brukernotifikasjonBrukervarselTopic,
                brukervarselConsumer
            )
        )
            .`as`("Skal ikke sendes melding")
            .isTrue()
    }

    fun assertDone(varselId: String?): ConsumerRecord<NokkelInput, DoneInput> {
        //Trigger scheduld jobb manuelt da schedule er disabled i test.
        config.avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner()
        val singleRecord = KafkaTestUtils.getSingleRecord(
            doneInputConsumer,
            config.brukernotifikasjonVarselHendelseTopic,
            KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION
        )
        val key = singleRecord.key()
        //        assertEquals(eventNokkel.getFodselsnummer(), key.getFodselsnummer());
        Assertions.assertEquals(varselId, key.eventId)
        Assertions.assertEquals(config.appname, key.appnavn)
        Assertions.assertEquals(config.namespace, key.namespace)

        return singleRecord
    }


    fun sendEksternVarseltOk(varsel: VarselDto) {
        sendVarsel(
            MinSideBrukernotifikasjonsId(UUID.fromString(varsel.varselId)),
            EksternVarslingKvitteringConsumer.FERDIGSTILT,
            varsel.type
        )
    }

    fun sendEksternVarseletFeilet(varsel: VarselDto) {
        sendVarsel(
            MinSideBrukernotifikasjonsId(UUID.fromString(varsel.varselId)),
            EksternVarslingKvitteringConsumer.FEILET,
            varsel.type
        )
    }

    @SneakyThrows
    fun sendVarsel(varselId: MinSideBrukernotifikasjonsId, status: String, varselType: Varseltype) {
        val kviteringsId = getKviteringsId(varselId, varselType)
        val doknot = doknotifikasjonStatus(kviteringsId, status)
        val result = kviteringsProducer.send(
            config.kviteringsToppic, kviteringsId, doknot
        ).get()
        kafkaTestService.assertErKonsumert(config.kviteringsToppic, result.recordMetadata.offset())
    }

    fun assertSkalIkkeHaProdusertFlereMeldinger() {
        config.avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner()
        config.sendBrukernotifikasjonCron.sendBrukernotifikasjoner()

        kafkaTestService.harKonsumertAlleMeldinger(config.brukernotifikasjonBrukervarselTopic, brukervarselConsumer)
        kafkaTestService.harKonsumertAlleMeldinger(config.brukernotifkasjonFerdigTopic, doneInputConsumer)
    }


    private fun doknotifikasjonStatus(bestillingsId: String, status: String): DoknotifikasjonStatus {
        return DoknotifikasjonStatus
            .newBuilder()
            .setStatus(status)
            .setBestillingsId(bestillingsId)
            .setBestillerId("veilarbaktivitet")
            .setMelding("her er en melding")
            .setDistribusjonId(1L)
            .build()
    }

    private fun getKviteringsId(varselId: MinSideBrukernotifikasjonsId, varselType: Varseltype): String {
        if (Varseltype.Beskjed == varselType) {
            return "B-" + config.appname + "-" + varselId.value.toString()
        }
        if (Varseltype.Oppgave == varselType) {
            return "O-" + config.appname + "-" + varselId.value.toString()
        }
        throw IllegalArgumentException("Kommer denne klassen fra brukernotifikasjoner?")
    }

    companion object {
        @JvmStatic
        val brukerSomIkkeKanVarsles: MockBruker
            get() = MockNavService.createBruker(BrukerOptions.happyBrukerBuilder().erManuell(true).build())
    }
}
