package no.nav.veilarbaktivitet.arkivering

import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService
import no.nav.veilarbaktivitet.aktivitet.HistorikkService
import no.nav.veilarbaktivitet.arkivering.Arkiveringslogikk.aktiviteterOgDialogerOppdatertEtter
import no.nav.veilarbaktivitet.arkivering.Arkiveringslogikk.lagArkivPayload
import no.nav.veilarbaktivitet.arkivering.Arkiveringslogikk.lagForhåndsvisningPayload
import no.nav.veilarbaktivitet.config.OppfolgingsperiodeResource
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.person.EksternNavnService
import no.nav.veilarbaktivitet.person.UserInContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

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
    @AuthorizeFnr(auditlogMessage = "lag forhåndsvisning av aktivitetsplan og dialog", resourceType = OppfolgingsperiodeResource::class, resourceIdParamName = "oppfolgingsperiodeId")
    fun forhaandsvisAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID): ForhaandsvisningOutboundDTO {
        val dataHentet = ZonedDateTime.now()
        val forhåndsvisningPayload = hentForhåndsvisningPayload(oppfølgingsperiodeId)

        val forhaandsvisningResultat = orkivarClient.hentPdfForForhaandsvisning(forhåndsvisningPayload)

        return ForhaandsvisningOutboundDTO(
            forhaandsvisningResultat.pdf,
            dataHentet,
            forhaandsvisningResultat.sistJournalført
        )
    }

    @PostMapping("/journalfor")
    @AuthorizeFnr(auditlogMessage = "journalføre aktivitetsplan og dialog", resourceType = OppfolgingsperiodeResource::class, resourceIdParamName = "oppfolgingsperiodeId")
    fun arkiverAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID, @RequestBody arkiverInboundDTO: ArkiverInboundDTO): JournalførtOutboundDTO {
        val arkivPayload = hentArkivPayload(oppfølgingsperiodeId, arkiverInboundDTO.journalforendeEnhet, arkiverInboundDTO.forhaandsvisningOpprettet)
        val journalførtResult = orkivarClient.journalfor(arkivPayload)
        return JournalførtOutboundDTO(
            sistJournalført = journalførtResult.sistJournalført
        )
    }

    private fun hentArkivPayload(oppfølgingsperiodeId: UUID, journalførendeEnhet: String, forhaandsvisningTidspunkt: ZonedDateTime): ArkivPayload {
        val fnr = userInContext.fnr.get()
        val oppfølgingsperiode = oppfølgingsperiodeService.hentOppfolgingsperiode(userInContext.aktorId, oppfølgingsperiodeId) ?: throw RuntimeException("Fant ingen oppfølgingsperiode for $oppfølgingsperiodeId")
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

        return lagArkivPayload(fnr, navn, oppfølgingsperiode, aktiviteter, dialoger, sak, mål, historikkForAktiviteter, journalførendeEnhet)
    }

    private fun hentForhåndsvisningPayload(oppfølgingsperiodeId: UUID): ForhåndsvisningPayload {
        val fnr = userInContext.fnr.get()
        val oppfølgingsperiode = oppfølgingsperiodeService.hentOppfolgingsperiode(userInContext.aktorId, oppfølgingsperiodeId) ?: throw RuntimeException("Fant ingen oppfølgingsperiode for $oppfølgingsperiodeId")
        val aktiviteter = appService.hentAktiviteterUtenKontorsperre(fnr)
        val dialoger = dialogClient.hentDialogerUtenKontorsperre(fnr)
        val navn = navnService.hentNavn(fnr)
        val mål = oppfølgingsperiodeService.hentMål(fnr)
        val historikkForAktiviteter = historikkService.hentHistorikk(aktiviteter.map { it.id })

        return lagForhåndsvisningPayload(fnr, navn, oppfølgingsperiode, aktiviteter, dialoger, mål, historikkForAktiviteter)
    }

    data class ForhaandsvisningOutboundDTO(
        val pdf: ByteArray,
        val forhaandsvisningOpprettet: ZonedDateTime,
        val sistJournalført: LocalDateTime?
    )

    data class ArkiverInboundDTO(
        val forhaandsvisningOpprettet: ZonedDateTime,
        val journalforendeEnhet: String
    )

    data class JournalførtOutboundDTO(
        val sistJournalført: LocalDateTime
    )
}
