package no.nav.veilarbaktivitet.admin

import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.person.Person
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/kassering")
class KasserController(
    private val authService: IAuthService,
    private val aktivitetDAO: AktivitetDAO,
    private val kasseringsService: KasseringsService,
    @Value("\${app.env.kassering.godkjenteIdenter}")
    private val godkjenteIdenter: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PutMapping("/{aktivitetId}")
    @ResponseStatus(value = HttpStatus.OK)
    fun kasserAktivitet(@PathVariable aktivitetId: String) {
        val id = aktivitetId.toLong()
        val aktivitetData = aktivitetDAO.hentAktivitet(id)
        val veilederIdent = Person.navIdent(authService.getInnloggetVeilederIdent().get())
        kjorHvisTilgang(aktivitetData.aktorId, aktivitetId) {
            kasseringsService.kasserAktivitet(aktivitetData, veilederIdent)
        }
    }

    private fun kjorHvisTilgang(aktorId: Person.AktorId, id: String, kasser: () -> Unit) {
        authService.sjekkInternbrukerHarSkriveTilgangTilPerson(aktorId.otherAktorId())
        val veilederIdent = authService.getInnloggetVeilederIdent()
        val godkjente = godkjenteIdenter.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        if (!godkjente.contains(veilederIdent.get())) {
            log.error(
                "[KASSERING] {} har ikke tilgang til kassering av {} aktivitet",
                veilederIdent,
                aktorId
            )
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                String.format("[KASSERING] %s har ikke tilgang til kassinger av %s aktivitet", veilederIdent, aktorId)
            )
        }

        kasser()

        log.info(
            "[KASSERING] {} kasserte en aktivitet. AktoerId: {} aktivitet_id: {}",
            veilederIdent,
            aktorId,
            id
        )
    }
}
