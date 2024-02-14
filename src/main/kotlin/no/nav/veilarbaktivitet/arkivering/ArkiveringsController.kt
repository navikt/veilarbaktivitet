package no.nav.veilarbaktivitet.arkivering

import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.arkivering.mapper.tilDialogTråd
import no.nav.veilarbaktivitet.arkivering.mapper.tilMelding
import no.nav.veilarbaktivitet.arkivering.mapper.toArkivPayload
import no.nav.veilarbaktivitet.person.EksternNavnService
import no.nav.veilarbaktivitet.person.UserInContext
import no.nav.veilarbaktivitet.util.DateUtils
import org.apache.coyote.Response
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties.Http
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.ZonedDateTime
import java.util.*
import kotlin.collections.List

@RestController
@RequestMapping("/api/arkivering")
class ArkiveringsController(
    private val userInContext: UserInContext,
    private val orkivarClient: OrkivarClient,
    private val dialogClient: DialogClient,
    private val navnService: EksternNavnService,
    private val appService: AktivitetAppService
) {
    @GetMapping("/forhaandsvisning")
    @AuthorizeFnr(auditlogMessage = "lag forhåndsvisning av aktivitetsplan og dialog")
    fun forhaandsvisAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID): ForhaandsvisningOutboundDTO {
        val dataHentet = ZonedDateTime.now()
        val fnr = userInContext.fnr.get()
        val result = navnService.hentNavn(fnr)
        if(result.errors?.isNotEmpty() == true) { throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR) }
        val navn = result.data.hentPerson.navn.first().tilFornavnMellomnavnEtternavn()

        val aktiviteter = appService.hentAktiviteterForIdentMedTilgangskontroll(fnr)
        val dialoger = dialogClient.hentDialoger(fnr)

        val (aktiviteterPayload, dialogerPayload) = lagDataTilOrkivar(oppfølgingsperiodeId, aktiviteter, dialoger)
        val forhaandsvisningResultat = orkivarClient.hentPdfForForhaandsvisning(fnr, navn, aktiviteterPayload, dialogerPayload)

        return ForhaandsvisningOutboundDTO(
            forhaandsvisningResultat.uuid,
            forhaandsvisningResultat.pdf,
            dataHentet
        )
    }

    @PostMapping("/journalfor")
    @AuthorizeFnr(auditlogMessage = "journalføre aktivitetsplan og dialog")
    fun arkiverAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID, @RequestBody arkiverInboundDTO: ArkiverInboundDTO) {
        val fnr = userInContext.fnr.get()

        val aktiviteter = appService.hentAktiviteterForIdentMedTilgangskontroll(fnr).filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }
        val dialoger = dialogClient.hentDialoger(fnr).filter { it.oppfolgingsperiode == oppfølgingsperiodeId }

        val oppdatertEtterForhaandsvisning = aktiviteterOgDialogerOppdatertEtter(arkiverInboundDTO.forhaandsvisningOpprettet, aktiviteter, dialoger)

        if(oppdatertEtterForhaandsvisning) throw ResponseStatusException(HttpStatus.CONFLICT)

        val result = navnService.hentNavn(fnr)
        if(result.errors?.isNotEmpty() == true) { throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR) }
        val navn = result.data.hentPerson.navn.first().tilFornavnMellomnavnEtternavn()

        val (aktiviteterPayload, dialogerPayload) = lagDataTilOrkivar(oppfølgingsperiodeId, aktiviteter, dialoger)
        orkivarClient.journalfor(fnr, navn, aktiviteterPayload, dialogerPayload)
    }


    data class ForhaandsvisningOutboundDTO(
        val uuid: UUID,
        val pdf: ByteArray,
        val dataHentet: ZonedDateTime
    )

    data class ArkiverInboundDTO(
        val uuid: UUID,
        val forhaandsvisningOpprettet: ZonedDateTime
    )
}
