package no.nav.veilarbaktivitet.arkivering

import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService
import no.nav.veilarbaktivitet.aktivitet.HistorikkService
import no.nav.veilarbaktivitet.arkivering.Arkiveringslogikk.aktiviteterOgDialogerOppdatertEtter
import no.nav.veilarbaktivitet.arkivering.Arkiveringslogikk.lagArkivPayload
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.person.EksternNavnService
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.Person.AktorId
import no.nav.veilarbaktivitet.person.Person.Fnr
import no.nav.veilarbaktivitet.person.PersonService
import no.nav.veilarbaktivitet.person.UserInContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

@RestController
@RequestMapping("/api/arkivering")
class ArkiveringsController(
    private val userInContext: UserInContext,
    private val orkivarClient: OrkivarClient,
    private val dialogClient: DialogClient,
    private val navnService: EksternNavnService,
    private val appService: AktivitetAppService,
    private val oppfølgingsperiodeService: OppfolgingsperiodeService,
    private val historikkService: HistorikkService,
) {
    @GetMapping("/forhaandsvisning")
    @AuthorizeFnr(auditlogMessage = "lag forhåndsvisning av aktivitetsplan og dialog")
    fun forhaandsvisAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID): ForhaandsvisningOutboundDTO {
        val dataHentet = ZonedDateTime.now()
        val fnr = userInContext.fnr.get()
        val arkivPayload = hentArkivPayload(fnr, oppfølgingsperiodeId)

        val forhaandsvisningResultat = orkivarClient.hentPdfForForhaandsvisning(arkivPayload)

        return ForhaandsvisningOutboundDTO(
            forhaandsvisningResultat.pdf,
            dataHentet,
            forhaandsvisningResultat.sistJournalført
        )
    }

    @PostMapping("/journalfor")
    @AuthorizeFnr(auditlogMessage = "journalføre aktivitetsplan og dialog")
    fun arkiverAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID, @RequestBody arkiverInboundDTO: ArkiverInboundDTO): JournalførtOutboundDTO {
        val fnr = userInContext.fnr.get()
        val arkivPayload = hentArkivPayload(fnr, oppfølgingsperiodeId, arkiverInboundDTO.forhaandsvisningOpprettet)
        val journalførtResult = orkivarClient.journalfor(arkivPayload)
        return JournalførtOutboundDTO(
            sistJournalført = journalførtResult.sistJournalført
        )
    }

    private fun hentArkivPayload(fnr: Fnr, oppfølgingsperiodeId: UUID, forhaandsvisningTidspunkt: ZonedDateTime? = null): ArkivPayload {
        val oppfølgingsperiode = hentOppfølgingsperiode(userInContext.aktorId, oppfølgingsperiodeId)
        val aktiviteter = appService.hentAktiviteterUtenKontorsperre(fnr)
        val dialoger = dialogClient.hentDialogerUtenKontorsperre(fnr)
        val navn = navnService.hentNavn(fnr)
        val sak = oppfølgingsperiodeService.hentSak(oppfølgingsperiodeId) ?: throw RuntimeException("Kunne ikke hente sak for oppfølgingsperiode: $oppfølgingsperiodeId")
        val mål = oppfølgingsperiodeService.hentMål(fnr)
        val historikkForAktiviteter = historikkService.hentHistorikk(aktiviteter.map { it.id })

        if (forhaandsvisningTidspunkt != null) {
            val oppdatertEtterForhaandsvisning = aktiviteterOgDialogerOppdatertEtter(forhaandsvisningTidspunkt, aktiviteter, dialoger)
            if (oppdatertEtterForhaandsvisning) throw ResponseStatusException(HttpStatus.CONFLICT)
        }

        return lagArkivPayload(fnr, navn, oppfølgingsperiode, aktiviteter, dialoger, sak, mål, historikkForAktiviteter)
    }

    private fun hentOppfølgingsperiode(aktorId: AktorId, oppfølgingsperiodeId: UUID): OppfolgingPeriodeMinimalDTO {
        return oppfølgingsperiodeService.hentOppfolgingsperiode(aktorId, oppfølgingsperiodeId) ?: throw RuntimeException("Fant ingen oppfølgingsperiode for $oppfølgingsperiodeId")
    }

    data class ForhaandsvisningOutboundDTO(
        val pdf: ByteArray,
        val forhaandsvisningOpprettet: ZonedDateTime,
        val sistJournalført: LocalDateTime?
    )

    data class ArkiverInboundDTO(
        val forhaandsvisningOpprettet: ZonedDateTime
    )

    data class JournalførtOutboundDTO(
        val sistJournalført: LocalDateTime
    )
}
