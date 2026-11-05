package no.nav.veilarbaktivitet.admin

import lombok.extern.slf4j.Slf4j
import no.nav.poao.dab.spring_auth.AuthService
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.person.Person
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Slf4j
@RestController
@RequestMapping("/admin")
class AdminController(
    val aktivitetService: AktivitetService,
    val periodeService: OppfolgingsperiodeService,
    val aktivitetDAO: AktivitetDAO,
    val authService: AuthService,
    @Value("\${app.env.kassering.godkjenteIdenter}")
    private val godkjenteIdenter: String
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/flytt-aktiviteter-til-siste-periode")
    fun flyttAktiviteter(@RequestBody personDto: PersonDto): Map<String, Int> {
        return kjorHvisTilgang {
            personDto.aktorIds.associateWith {
                flyttAktiviteter(it)
            }
        }
    }

    private fun flyttAktiviteter(aktorIdString: String): Int {
        val aktorId = Person.AktorId(aktorIdString)
        val sortertePerioder = periodeService.hentOppfolgingsPerioder(aktorId)
            .sortedBy { it.startTid }

        val sisteÅpenPeriodeIndex = sortertePerioder.indexOfLast { it.sluttTid == null }
        val sisteÅpenPeriode = sortertePerioder.getOrNull(sisteÅpenPeriodeIndex)
            ?: throw RuntimeException("Fant ikke noe åpen periode på person, kan ikke flytte aktiviteter.")
        val nestSistePeriode = sortertePerioder.getOrNull(sisteÅpenPeriodeIndex - 1)
            ?: throw RuntimeException("Fant ikke noe nest siste periode på person, kan ikke flytte aktiviteter.")
        val aktiviteterINestSistePeriode = aktivitetService.hentAktiviteterForAktorId(aktorId)
            .filter { it.oppfolgingsperiodeId == nestSistePeriode.oppfolgingsperiodeId }
            .filter { it.status != AktivitetStatus.AVBRUTT && it.status != AktivitetStatus.FULLFORT }
            .map {
                try {
                    aktivitetDAO.skiftPeriodePåAktivitet(it.id, sisteÅpenPeriode.oppfolgingsperiodeId)
                } catch (e: Exception) {
                    logger.error(
                        "Feilet ved flytting av aktivitet ${it.id} fra periode ${nestSistePeriode.oppfolgingsperiodeId} til periode ${sisteÅpenPeriode.oppfolgingsperiodeId}",
                        e
                    )
                }
            }
        logger.info("Flyttet ${aktiviteterINestSistePeriode.size} aktiviteter fra periode ${nestSistePeriode.oppfolgingsperiodeId} til periode ${sisteÅpenPeriode.oppfolgingsperiodeId}")
        return aktiviteterINestSistePeriode.size
    }

    private fun kjorHvisTilgang(callback: () -> Map<String, Int>): Map<String, Int> {
        val veilederIdent = authService.getInnloggetVeilederIdent()
        val godkjente = godkjenteIdenter.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        if (!godkjente.contains(veilederIdent.get())) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "${veilederIdent} har ikke tilgang til flytte aktiviteter"
            )
        }

        return callback()
    }

}

data class PersonDto(
    val aktorIds: List<String>,
)

