package no.nav.veilarbaktivitet.aktivitetskort.service

import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client
import no.nav.veilarbaktivitet.person.Person.AktorId
import okhttp3.internal.toImmutableList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.chrono.ChronoZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@Service
class OppfolgingsperiodeService(
	private val oppfolgingClient: OppfolgingV2Client
) {
	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		val SLACK_FOER: Duration = Duration.ofDays(7)
	}

	fun finnOppfolgingsperiode(aktorId: AktorId, opprettetTidspunkt: LocalDateTime): OppfolgingPeriodeMinimalDTO? {
		val oppfolgingsperioder = oppfolgingClient.hentOppfolgingsperioder(aktorId).toImmutableList()
			.sortedByDescending { it.startDato }

		if (oppfolgingsperioder.isEmpty()) {
			log.info("Arenatiltak finn oppfølgingsperiode - bruker har ingen oppfølgingsperioder - aktorId=${aktorId.get()}, opprettetTidspunkt=${opprettetTidspunkt}, oppfolgingsperioder=${listOf<OppfolgingPeriodeMinimalDTO>()}")
			return null
		}

		fun OppfolgingPeriodeMinimalDTO.erInnenforPeriode(opprettetTidspunktCZDT: ZonedDateTime): Boolean {
			return this.startDato.isBeforeOrEqual(opprettetTidspunktCZDT) &&
					(this.sluttDato?.isAfter(opprettetTidspunktCZDT) ?: true)
		}

		fun OppfolgingPeriodeMinimalDTO.erInnenforMedEkstraSlack(opprettetTidspunktCZDT: ZonedDateTime): Boolean {
			val utvidetOppfolgingsperiode = OppfolgingPeriodeMinimalDTO(
				this.uuid,
				this.startDato.minus(SLACK_FOER),
				this.sluttDato
			)
			return utvidetOppfolgingsperiode.erInnenforPeriode(opprettetTidspunktCZDT)
		}

		val opprettetTidspunktCZDT = opprettetTidspunkt.atZone(ZoneId.systemDefault())
		val match = oppfolgingsperioder
			.firstOrNull { oppfolgingsperiode -> oppfolgingsperiode.erInnenforPeriode(opprettetTidspunktCZDT) }
			?: oppfolgingsperioder
				.filter { oppfolgingsperiode -> oppfolgingsperiode.erInnenforMedEkstraSlack(opprettetTidspunktCZDT) }
				.minByOrNull { abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, it.startDato)) }
				?.also { _ ->
					log.info("Arenatiltak finn oppfølgingsperiode - opprettetdato innen 1 uke før oppfølging startdato) - aktorId=${aktorId.get()}, opprettetTidspunkt=${opprettetTidspunkt}, oppfolgingsperioder=${oppfolgingsperioder}")
				}

		return if (match != null) {
			match
		} else {
			log.info("Arenatiltak finn oppfølgingsperiode - opprettetTidspunkt har ingen god match på oppfølgingsperioder) - aktorId=${aktorId.get()}, opprettetTidspunkt=${opprettetTidspunkt}, oppfolgingsperioder=${oppfolgingsperioder}")
			null
		}
	}



	private fun ZonedDateTime.isBeforeOrEqual(other: ChronoZonedDateTime<*>): Boolean {
		return this.isBefore(other) || this.isEqual(other)
	}
}
