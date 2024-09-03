package no.nav.veilarbaktivitet.admin

import no.nav.common.utils.EnvironmentUtils
import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.config.ApplicationContext
import no.nav.veilarbaktivitet.internapi.model.Status
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.person.Person
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.Date
import java.util.function.BooleanSupplier

@RestController
@RequestMapping("/api/kassering")
class KasserController(
    private val authService: IAuthService,
    private val aktivitetDAO: AktivitetDAO,
    private val kasseringDAO: KasseringDAO,
    @Value("\${app.env.kassering.godkjenteIdenter}")
    private val godkjenteIdenter: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PutMapping("/{aktivitetId}")
    @ResponseStatus(value = HttpStatus.OK)
    fun kasserAktivitet(@PathVariable("aktivitetId") aktivitetId: String) {
        val id = aktivitetId.toLong()
        val aktivitetData = aktivitetDAO.hentAktivitet(id)
        val veilederIdent = Person.navIdent(authService.getInnloggetVeilederIdent().get())
        kjorHvisTilgang(aktivitetData.aktorId, aktivitetId) {
            aktivitetData.withEndretAv(veilederIdent.get())
            aktivitetData.withEndretDato(Date())
            aktivitetData.withEndretAvType(Innsender.NAV)
            aktivitetData.withStatus(AktivitetStatus.AVBRUTT)
            aktivitetDAO.oppdaterAktivitet(aktivitetData)
            kasseringDAO.kasserAktivitet(
                id,
                veilederIdent
            )
        }
    }

    private fun kjorHvisTilgang(aktorId: Person.AktorId, id: String, fn: BooleanSupplier): Boolean {
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
        val updated = fn.asBoolean
        log.info(
            "[KASSERING] {} kasserte en aktivitet. AktoerId: {} aktivitet_id: {}",
            veilederIdent,
            aktorId,
            id
        )
        return updated
    }
}
