package no.nav.veilarbaktivitet.brukernotifikasjon

import junit.framework.TestCase.assertEquals
import no.nav.tms.varsel.action.Varseltype
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.InaktiverVarselDto
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.MinSideVarselId
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternStatusOppdatertEventName
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselKanal
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.EksternVarselStatus
import no.nav.veilarbaktivitet.brukernotifikasjon.varselStatusHendelse.InternVarselHendelseType
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
            varsel.type,
            kanal = EksternVarselKanal.SMS
        )
    }

    fun simulerEksternVarselStatusFeilet(varsel: OpprettVarselDto) {
        simulerEksternVarselStatusHendelse(
            MinSideVarselId(UUID.fromString(varsel.varselId)),
            EksternVarselStatus.feilet,
            varsel.type,
            null
        )
    }

    fun simulerEksternVarselStatusKansellert(varsel: OpprettVarselDto) {
        simulerEksternVarselStatusHendelse(
            MinSideVarselId(UUID.fromString(varsel.varselId)),
            EksternVarselStatus.kansellert,
            varsel.type,
            null
        )
    }

    fun simulerEksternVarselStatusFerdigstilt(varsel: OpprettVarselDto) {
        simulerEksternVarselStatusHendelse(
            MinSideVarselId(UUID.fromString(varsel.varselId)),
            EksternVarselStatus.ferdigstilt,
            varsel.type,
            null
        )
    }

    private fun simulerEksternVarselStatusHendelse(varselId: MinSideVarselId, status: EksternVarselStatus, varselType: Varseltype, kanal: EksternVarselKanal?) {
        val eksternVarsel = eksternVarselHendelse(varselId, status, varselType, kanal)
        simulerVarselHendelse(varselId, eksternVarsel)
    }

    fun simulerInternVarselStatusHendelse(varselId: MinSideVarselId, internVarselHendelseType: InternVarselHendelseType, varselType: Varseltype) {
        val internVarsel = internVarselHendelse(varselId, internVarselHendelseType, varselType)
        simulerVarselHendelse(varselId, internVarsel)
    }

    fun simulerVarselFraAnnenApp() {
        val annenAppSittVarsel = internVarselHendelse(MinSideVarselId(UUID.randomUUID()), InternVarselHendelseType.opprettet, Varseltype.Beskjed)
            .copy(appnavn = "annen app")
        simulerVarselHendelse(MinSideVarselId(annenAppSittVarsel.varselId), annenAppSittVarsel)
    }

    private fun simulerVarselHendelse(varselId: MinSideVarselId, hendelse: TestVarselHendelseDTO) {
        val result = brukernotifikasjonVarselHendelseProducer.publiserBrukernotifikasjonVarselHendelse(
            varselId, hendelse
        )
        kafkaTestService.assertErKonsumert(brukernotifikasjonVarselHendelseProducer.topic, result.recordMetadata.offset())
    }

    fun assertSkalIkkeHaProdusertFlereMeldinger() {
        config.avsluttBrukernotifikasjonCron.avsluttBrukernotifikasjoner()
        config.sendMinsideVarselFraOutboxCron.sendBrukernotifikasjoner()
        kafkaTestService.harKonsumertAlleMeldinger(config.brukernotifikasjonBrukervarselTopic, brukervarselConsumer.consumer)
    }

    private fun eksternVarselHendelse(varselId: MinSideVarselId, status: EksternVarselStatus, varselType: Varseltype, kanal: EksternVarselKanal?): EksternVarselHendelseDTO {
        return EksternVarselHendelseDTO(
            namespace = config.namespace,
            appnavn = config.appname,
            eventName = EksternStatusOppdatertEventName,
            status = status,
            varselId = varselId.value,
            varseltype = varselType,
            kanal = kanal
        )
    }

    private fun internVarselHendelse(varselId: MinSideVarselId, hendelseType: InternVarselHendelseType, varselType: Varseltype): InternVarselHendelseDTO {
        return InternVarselHendelseDTO(
            namespace = config.namespace,
            appnavn = config.appname,
            varseltype = varselType,
            varselId = varselId.value,
            eventName = hendelseType.name,
        )
    }
}
