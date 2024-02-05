package no.nav.veilarbaktivitet.arkivering

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService
import no.nav.veilarbaktivitet.arkivering.mapper.norskDato
import no.nav.veilarbaktivitet.arkivering.mapper.toArkivPayload
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.oppfolging.periode.SistePeriodeService
import no.nav.veilarbaktivitet.person.EksternNavnService
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.UserInContext
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/arkivering")
class ArkiveringsController(
    private val userInContext: UserInContext,
    private val orkivarClient: OrkivarClient,
    private val dialogClient: DialogClient,
    private val navnService: EksternNavnService,
    private val appService: AktivitetAppService,
    private val aktorOppslagClient: AktorOppslagClient,
    private val sisteOppfølgingsperiodeService: SistePeriodeService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // TODO: Ta i mot oppfølgingsperiode fra klienten
    @PostMapping
    @AuthorizeFnr(auditlogMessage = "arkivere aktivitetsplan og dialog")
    fun arkiverAktivitetsplanOgDialog() {
        val fnr = userInContext.fnr.get()
        val result = navnService.hentNavn(fnr)
        if(result.errors?.isNotEmpty() == true) { throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR) }
        val navn = result.data.hentPerson.navn.first().tilFornavnMellomnavnEtternavn()

        val aktorId = aktorOppslagClient.hentAktorId(fnr.otherFnr())
        // TODO: Ikke hardkod oppfølgingsperiode
        val oppfølgingsperiodeUuid = sisteOppfølgingsperiodeService.hentGjeldendeOppfolgingsperiodeMedFallback(
            Person.AktorId(aktorId.get()))

        val dialoger = dialogClient.hentDialoger(fnr)

        val aktiviteter = appService.hentAktiviteterForIdentMedTilgangskontroll(fnr)
        val aktivitetDialoger = dialoger.groupBy { it.aktivitetId }


        val aktiviteterPayload = aktiviteter
            .map {
                it.toArkivPayload(
                    meldinger = aktivitetDialoger[it.oppfolgingsperiodeId].map { it.tilTråd().meldinger }
                )
            }
        orkivarClient.arkiver(fnr, navn, aktiviteterPayload, aktivitetDialoger[null].map {  })
    }
}

fun DialogClient.TrådDTO.tilTråd() =
    Tråd(
        overskrift = overskrift,
        egenskaper = egenskaper.map { it.toString() },
        meldinger =  meldinger.map { dialog ->
            Melding(
                avsender = dialog.avsender.toString(), // TODO: Gjør mapping annet sted og slå sammen ident hvis veileder
                sendt = dialog.sendt.norskDato(), // TODO: Klokkeslett også
                lest = dialog.lest,
                viktig = dialog.viktig,
                tekst = dialog.tekst
            )
        }
    )

