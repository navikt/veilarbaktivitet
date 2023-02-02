package no.nav.veilarbaktivitet.aktivitetskort.service

import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client
import no.nav.veilarbaktivitet.person.Person.AktorId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.ChronoZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Comparator.comparingLong
import kotlin.math.abs

@Service
open class OppfolgingsperiodeService(
	private val oppfolgingClient: OppfolgingV2Client
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun innenEnUke(opprettetTidspunkt: LocalDateTime, periodeStartDato: ZonedDateTime ): Boolean {
		return opprettetTidspunkt.plus(7, ChronoUnit.DAYS).isAfter(periodeStartDato.toLocalDateTime())
	}

	fun finnOppfolgingsperiode(aktorId: AktorId, opprettetTidspunkt: LocalDateTime): OppfolgingPeriodeMinimalDTO? {
		val oppfolgingsperioder = oppfolgingClient.hentOppfolgingsperioder(aktorId)

		if (oppfolgingsperioder.isEmpty()) {
			log.info(
				"Arenatiltak finn oppfølgingsperiode - bruker har ingen oppfølgingsperioder - aktorId={}, opprettetTidspunkt={}, oppfolgingsperioder={}",
				aktorId.get(),
				opprettetTidspunkt,
				listOf<OppfolgingPeriodeMinimalDTO>()
			)
			return null
		}

		val oppfolgingsperioderCopy = oppfolgingsperioder.toMutableList()
		oppfolgingsperioderCopy.sortWith(Comparator.comparing(OppfolgingPeriodeMinimalDTO::startDato).reversed())

		val opprettetTidspunktCZDT = ChronoZonedDateTime.from(opprettetTidspunkt.atZone(ZoneId.systemDefault()))
		val maybePeriode = oppfolgingsperioderCopy
			.stream()
			.filter {
				((it.startDato.isBefore(opprettetTidspunktCZDT) || it.startDato.isEqual(opprettetTidspunktCZDT)) && it.sluttDato == null) ||
					((it.startDato.isBefore(opprettetTidspunktCZDT) || it.startDato.isEqual(opprettetTidspunktCZDT)) && it.sluttDato!!.isAfter(
						opprettetTidspunktCZDT
					))
			}
			.findFirst()

		return maybePeriode.orElseGet {
			oppfolgingsperioderCopy
				.stream()
				.filter { it.sluttDato == null || it.sluttDato.isAfter(opprettetTidspunktCZDT) }
				.min(comparingLong { abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, it.startDato)) })
				.filter {
					val innenEnUke = innenEnUke(opprettetTidspunkt, it.startDato)
					if (innenEnUke) {
						log.info(
							"Arenatiltak finn oppfølgingsperiode - opprettetdato innen 1 uke oppfølging startdato) - aktorId={}, opprettetTidspunkt={}, oppfolgingsperioder={}",
							aktorId.get(),
							opprettetTidspunkt,
							oppfolgingsperioder
						)
					}
					innenEnUke
				}.orElseGet {
					log.info(
						"Arenatiltak finn oppfølgingsperiode - opprettetTidspunkt har ingen god match på oppfølgingsperioder) - aktorId={}, opprettetTidspunkt={}, oppfolgingsperioder={}",
						aktorId.get(),
						opprettetTidspunkt,
						oppfolgingsperioder
					)
					null
				}
		}
	}
}
