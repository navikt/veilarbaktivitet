package no.nav.veilarbaktivitet.admin

import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.person.Person
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Slf4j
@RestController
@RequestMapping("/admin")
class AdminController(
    val aktivitetService: AktivitetService,
    val periodeService: OppfolgingsperiodeService,
    val aktivitetDAO: AktivitetDAO
) {
    private val logger = LoggerFactory.getLogger(AdminController::class.java)

    @PostMapping("/flytt-aktiviteter-til-siste-periode")
    fun flyttAktiviteter(@RequestBody personDto: PersonDto): Map<String, Int> {
        return personDto.aktorIds.associateWith {
            flyttAktiviteter(it)
        }
    }

    private fun flyttAktiviteter(aktorIdString: String): Int {
        val aktorId = Person.AktorId(aktorIdString)
        val perioder = periodeService.hentOppfolgingsPerioder(aktorId)
        val sisteÅpenPeriode = perioder.singleOrNull { it.sluttTid == null }
        val nestSistePeriode = perioder.getOrNull(perioder.size - 2)
        if (sisteÅpenPeriode == null) {
            throw RuntimeException("Fant ikke noe åpen periode på person, kan ikke flytte aktiviteter.")
        }
        if (nestSistePeriode == null) {
            throw RuntimeException("Fant ikke noe nest siste periode på person, kan ikke flytte aktiviteter.")
        }
        val aktiviteterINestSistePeriode = aktivitetService.hentAktiviteterForAktorId(aktorId)
            .filter { it.oppfolgingsperiodeId == nestSistePeriode.oppfolgingsperiodeId }
            .filter { it.status != AktivitetStatus.AVBRUTT && it.status != AktivitetStatus.FULLFORT }
            .map {
                try {
                    // TODO: Slå på denne når vi går i prod
//                    aktivitetDAO.skiftPeriodePåAktivitet(it.id, sisteÅpenPeriode.oppfolgingsperiodeId)
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
}

data class PersonDto(
    val aktorIds: List<String>,
)

