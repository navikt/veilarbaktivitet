package no.nav.veilarbaktivitet.brukernotifikasjon

import junit.framework.TestCase.assertEquals
import lombok.SneakyThrows
import no.nav.tms.varsel.action.Varseltype
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.InaktiverVarselDto
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideBrukernotifikasjonsId
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselHendelseDTO
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselStatus
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.util.KafkaTestService
import org.assertj.core.api.Assertions.assertThat
import java.util.*

class BrukernotifikasjonAsserts(var config: BrukernotifikasjonAssertsConfig) {
    var brukervarselConsumer = config.createBrukerVarselConsumer()
    private val brukernotifikasjonVarselHendelseProducer = config.brukernotifikasjonVarselProducer()

    var kafkaTestService: KafkaTestService = config.testService

    fun assertProdusentErRiktig(varsel: OpprettVarselDto) {
        assertEquals(config.appname, varsel.getProdusent().getAppnavn())
        assertEquals(config.namespace, varsel.getProdusent().getNamespace())
    }

    fun assertOppgaveSendt(fnr: Person.Fnr): OpprettVarselDto {
        config.sendBrukernotifikasjonCron.sendBrukernotifikasjoner()
        val varsel = brukervarselConsumer.waitForOpprettVarsel()
        assertEquals(fnr.get(), varsel.getIdent())
        assertEquals(Varseltype.Oppgave, varsel.type)
        assertProdusentErRiktig(varsel)
        return varsel
    }

    fun assertBeskjedSendt(fnr: Person.Fnr): OpprettVarselDto {
        config.sendBrukernotifikasjonCron.sendBrukernotifikasjoner()
        val varsel = brukervarselConsumer.waitForOpprettVarsel()
        assertEquals(fnr.get(), varsel.getIdent())
        assertEquals(Varseltype.Beskjed, varsel.type)
        assertProdusentErRiktig(varsel)
        return varsel
    }

    fun assertIngenNyeBeskjeder() {
        config.sendBrukernotifikasjonCron.sendBrukernotifikasjoner()
        assertThat(
            kafkaTestService.harKonsumertAlleMeldinger(
                config.brukernotifikasjonBrukervarselTopic,
                brukervarselConsumer.consumer
            )
        )
            .`as`("Skal ikke sendes melding")
            .isTrue()
    }

    fun assertInaktivert(varselId: String?): InaktiverVarselDto {
        //Trigger scheduld jobb manuelt da schedule er disabled i test.
        config.avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner()
        val inaktivering = brukervarselConsumer.waitForInaktiverVarsel()
        assertEquals(varselId, inaktivering.varselId)
        assertEquals(config.appname, inaktivering.produsent.appnavn)
        assertEquals(config.namespace, inaktivering.produsent.namespace)
        return inaktivering
    }


    fun simulerEksternVarselSendt(varsel: OpprettVarselDto) {
        simulerEksternVarselStatusHendelse(
            MinSideBrukernotifikasjonsId(UUID.fromString(varsel.varselId)),
            EksternVarselStatus.sendt,
            varsel.type
        )
    }

    fun simulerEksternVarselFeilet(varsel: OpprettVarselDto) {
        simulerEksternVarselStatusHendelse(
            MinSideBrukernotifikasjonsId(UUID.fromString(varsel.varselId)),
            EksternVarselStatus.feilet,
            varsel.type
        )
    }

    @SneakyThrows
    fun simulerEksternVarselStatusHendelse(varselId: MinSideBrukernotifikasjonsId, status: EksternVarselStatus, varselType: Varseltype) {
        val eksternVarsel = eksternVarselHendelse(varselId, status, varselType)
        val result = brukernotifikasjonVarselHendelseProducer.publiserBrukernotifikasjonVarselHendelse(
            varselId, eksternVarsel
        )
        kafkaTestService.assertErKonsumert(brukernotifikasjonVarselHendelseProducer.topic, result.recordMetadata.offset())
    }

    fun assertSkalIkkeHaProdusertFlereMeldinger() {
        config.avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner()
        config.sendBrukernotifikasjonCron.sendBrukernotifikasjoner()
        kafkaTestService.harKonsumertAlleMeldinger(config.brukernotifikasjonBrukervarselTopic, brukervarselConsumer.consumer)
    }

    private fun eksternVarselHendelse(varselId: MinSideBrukernotifikasjonsId, status: EksternVarselStatus, varselType: Varseltype): EksternVarselHendelseDTO {
        return EksternVarselHendelseDTO(
            namespace = config.namespace,
            appnavn = config.appname,
            eventName = "eksternStatusOppdatert",
            status = status,
            varselId = varselId.value,
            varseltype = varselType.name
        )
    }
}
