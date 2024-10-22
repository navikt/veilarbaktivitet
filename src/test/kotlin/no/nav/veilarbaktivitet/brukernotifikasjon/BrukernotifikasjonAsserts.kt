package no.nav.veilarbaktivitet.brukernotifikasjon

import junit.framework.TestCase.assertEquals
import lombok.SneakyThrows
import no.nav.tms.varsel.action.Varseltype
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.InaktiverVarselDto
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideVarselId
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselHendelseDTO
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselStatus
import no.nav.veilarbaktivitet.person.Person.Fnr
import no.nav.veilarbaktivitet.util.KafkaTestService
import org.assertj.core.api.Assertions.assertThat
import java.util.*

class BrukernotifikasjonAsserts(var config: BrukernotifikasjonAssertsConfig) {
    var brukervarselConsumer = config.createBrukerVarselConsumer(config.testService)
    private val brukernotifikasjonVarselHendelseProducer = config.brukernotifikasjonVarselProducer()

    var kafkaTestService: KafkaTestService = config.testService

    private fun assertVarselSendt(fnr: Fnr, varselType: Varseltype): OpprettVarselDto {
        config.sendMinsideVarselFraOutboxCron.sendBrukernotifikasjoner()
        val varsel = brukervarselConsumer.waitForOpprettVarsel()
        assertEquals(fnr.get(), varsel.ident)
        assertEquals(varselType, varsel.type)
        assertEquals(config.appname, varsel.produsent.getAppnavn())
        assertEquals(config.namespace, varsel.produsent.getNamespace())
        return varsel
    }
    fun assertOppgaveSendt(fnr: Fnr) = assertVarselSendt(fnr, Varseltype.Oppgave)
    fun assertBeskjedSendt(fnr: Fnr) = assertVarselSendt(fnr, Varseltype.Beskjed)

    fun assertIngenNyeVarslerErIOutbox() {
        assertThat(config.sendMinsideVarselFraOutboxCron.sendAlle(500))
            .isEqualTo(0);
    }

    fun assertInaktivertMeldingErSendt(varselId: String?): InaktiverVarselDto {
        //Trigger scheduld jobb manuelt da schedule er disabled i test.
        config.avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner()
        val inaktivering = brukervarselConsumer.waitForInaktiverVarsel()
        assertEquals(varselId, inaktivering.varselId)
        assertEquals(config.appname, inaktivering.produsent.appnavn)
        assertEquals(config.namespace, inaktivering.produsent.namespace)
        return inaktivering
    }

    fun simulerEksternVarselStatusSendt(varsel: OpprettVarselDto) {
        simulerEksternVarselStatusHendelse(
            MinSideVarselId(UUID.fromString(varsel.varselId)),
            EksternVarselStatus.sendt,
            varsel.type
        )
    }

    fun simulerEksternVarselStatusFeilet(varsel: OpprettVarselDto) {
        simulerEksternVarselStatusHendelse(
            MinSideVarselId(UUID.fromString(varsel.varselId)),
            EksternVarselStatus.feilet,
            varsel.type
        )
    }

    @SneakyThrows
    private fun simulerEksternVarselStatusHendelse(varselId: MinSideVarselId, status: EksternVarselStatus, varselType: Varseltype) {
        val eksternVarsel = eksternVarselHendelse(varselId, status, varselType)
        val result = brukernotifikasjonVarselHendelseProducer.publiserBrukernotifikasjonVarselHendelse(
            varselId, eksternVarsel
        )
        kafkaTestService.assertErKonsumert(brukernotifikasjonVarselHendelseProducer.topic, result.recordMetadata.offset())
    }

    fun assertSkalIkkeHaProdusertFlereMeldinger() {
        config.avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner()
        config.sendMinsideVarselFraOutboxCron.sendBrukernotifikasjoner()
        kafkaTestService.harKonsumertAlleMeldinger(config.brukernotifikasjonBrukervarselTopic, brukervarselConsumer.consumer)
    }

    private fun eksternVarselHendelse(varselId: MinSideVarselId, status: EksternVarselStatus, varselType: Varseltype): EksternVarselHendelseDTO {
        return EksternVarselHendelseDTO(
            namespace = config.namespace,
            appnavn = config.appname,
            eventName = "eksternStatusOppdatert",
            status = status,
            varselId = varselId.value,
            varseltype = varselType
        )
    }
}
