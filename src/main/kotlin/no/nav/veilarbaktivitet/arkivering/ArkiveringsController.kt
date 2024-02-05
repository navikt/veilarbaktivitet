package no.nav.veilarbaktivitet.arkivering

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService
import no.nav.veilarbaktivitet.arkivering.mapper.norskDato
import no.nav.veilarbaktivitet.arkivering.mapper.toArkivPayload
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
        val aktiviteterIOppfølgingsperioden = appService.hentAktiviteterForIdentMedTilgangskontroll(fnr).filter { it.oppfolgingsperiodeId == oppfølgingsperiodeUuid }
        val dialogerIOppfølgingsperioden = dialogClient.hentDialoger(fnr).filter { it.oppfolgingsperiode == oppfølgingsperiodeUuid }

        val aktivitetDialoger = dialogerIOppfølgingsperioden.groupBy { it.aktivitetId }

        val aktiviteterPayload = aktiviteterIOppfølgingsperioden
            .map { it ->
                val meldingerTilhørendeAktiviteten = aktivitetDialoger[it.oppfolgingsperiodeId.toString()]?.map {
                    it.meldinger.map { it.tilMelding() }
                }?.flatten() ?: emptyList()

                it.toArkivPayload(
                    meldinger = meldingerTilhørendeAktiviteten
                )
            }

        val meldingerUtenAktivitet = aktivitetDialoger[null] ?: emptyList()
        orkivarClient.arkiver(fnr, navn, aktiviteterPayload, meldingerUtenAktivitet.map { it.tilDialogTråd() })
    }

    fun DialogClient.DialogTrådDTO.tilDialogTråd() =
        DialogTråd(
            overskrift = overskrift,
            egenskaper = egenskaper.map { it.toString() },
            meldinger =  meldinger.map { it.tilMelding()
            }
        )

    fun DialogClient.MeldingDTO.tilMelding() =
        Melding(
            avsender = avsender.toString(), // TODO: Gjør mapping annet sted og slå sammen ident hvis veileder
            sendt = sendt.norskDato(), // TODO: Klokkeslett også
            lest = lest,
            viktig = viktig,
            tekst = tekst
        )
}

