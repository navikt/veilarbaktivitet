package no.nav.veilarbaktivitet.aktivitetskort.service

import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client
import no.nav.veilarbaktivitet.person.Person.AktorId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.ChronoZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Comparator.comparingLong
import kotlin.math.abs

@Service
class OppfolgingsperiodeService(
	private val oppfolgingClient: OppfolgingV2Client
) {
	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		val SLACK_FOER: Duration = Duration.ofDays(7)
		val SLACK_ETTER: Duration = Duration.ofDays(7)
	}

	fun finnOppfolgingsperiode(aktorId: AktorId, opprettetTidspunkt: LocalDateTime): OppfolgingPeriodeMinimalDTO? {
		val oppfolgingsperioder = oppfolgingClient.hentOppfolgingsperioder(aktorId)

		if (oppfolgingsperioder.isEmpty()) {
			log.info( "Arenatiltak finn oppfølgingsperiode - bruker har ingen oppfølgingsperioder - aktorId=${aktorId.get()}, opprettetTidspunkt=${opprettetTidspunkt}, oppfolgingsperioder=${listOf<OppfolgingPeriodeMinimalDTO>()}")
			return null
		}

		val oppfolgingsperioderCopy = oppfolgingsperioder.toMutableList()
		oppfolgingsperioderCopy.sortWith(Comparator.comparing(OppfolgingPeriodeMinimalDTO::startDato).reversed())

		val opprettetTidspunktCZDT = ChronoZonedDateTime.from(opprettetTidspunkt.atZone(ZoneId.systemDefault()))
		val maybePeriode = oppfolgingsperioderCopy
			.filter {
				(it.startDato.isBeforeOrEqual(opprettetTidspunktCZDT) && it.sluttDato == null) ||
					(it.startDato.isBeforeOrEqual(opprettetTidspunktCZDT) && it.sluttDato!!.isAfter(opprettetTidspunktCZDT))
			}
			.firstOrNull()

		return maybePeriode.let {
			oppfolgingsperioderCopy
				.stream()
				.filter { it.sluttDato == null || it.sluttDato.isAfter(opprettetTidspunktCZDT.minus(SLACK_ETTER)) } // Tiltak som er opprettet etter oppfølgingsperiode slutt
				.min(comparingLong { abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, it.startDato)) })
				.filter {
					val predicate = it.startDato.minus(SLACK_FOER).isBefore(opprettetTidspunktCZDT) // Skal ta inn tiltak som er opprettet før oppfølgingsperiode start
					if (predicate) {
						log.info( "Arenatiltak finn oppfølgingsperiode - opprettetdato innen 1 uke oppfølging startdato) - aktorId=${aktorId.get()}, opprettetTidspunkt=${opprettetTidspunkt}, oppfolgingsperioder=${oppfolgingsperioder}")
					}
					predicate
				}.orElseGet {
					log.info("Arenatiltak finn oppfølgingsperiode - opprettetTidspunkt har ingen god match på oppfølgingsperioder) - aktorId=${aktorId.get()}, opprettetTidspunkt=${opprettetTidspunkt}, oppfolgingsperioder=${oppfolgingsperioder}")
					null
				}
		}
	}

	private fun ZonedDateTime.isBeforeOrEqual(other: ChronoZonedDateTime<*>): Boolean {
		return this.isBefore(other) || this.isEqual(other)
	}
}
