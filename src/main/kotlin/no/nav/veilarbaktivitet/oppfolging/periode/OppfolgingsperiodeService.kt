package no.nav.veilarbaktivitet.oppfolging.periode

import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client
import no.nav.veilarbaktivitet.person.Person.AktorId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

@Service
class OppfolgingsperiodeService(
	private val aktivitetService: AktivitetService,
	private val brukernotifikasjonService: BrukernotifikasjonService,
	private val sistePeriodeDAO: SistePeriodeDAO,
	private val oppfolgingsperiodeDAO: OppfolgingsperiodeDAO,
	private val oppfolgingClient: OppfolgingV2Client
) {
	private val log = LoggerFactory.getLogger(javaClass)

	companion object {
		val SLACK_FOER: Duration = Duration.ofDays(7)
	}

	fun avsluttOppfolgingsperiode(oppfolgingsperiode: UUID, sluttDato: ZonedDateTime) {
		brukernotifikasjonService.setDoneGrupperingsID(oppfolgingsperiode)
		aktivitetService.settAktiviteterTilHistoriske(oppfolgingsperiode, sluttDato)
	}

	fun upsertOppfolgingsperiode(sisteOppfolgingsperiodeV1: SisteOppfolgingsperiodeV1) {
		val oppfolgingsperiode = Oppfolgingsperiode(
			sisteOppfolgingsperiodeV1.aktorId,
			sisteOppfolgingsperiodeV1.uuid,
			sisteOppfolgingsperiodeV1.startDato,
			sisteOppfolgingsperiodeV1.sluttDato)
		oppfolgingsperiodeDAO.upsertOppfolgingsperide(oppfolgingsperiode)
		sistePeriodeDAO.uppsertOppfolingsperide(oppfolgingsperiode)
	}

	fun hentOppfolgingsperiode(aktorId: AktorId, oppfolgingsperiode: UUID): OppfolgingPeriodeMinimalDTO? {
		return oppfolgingClient.hentOppfolgingsperioder(aktorId).find { it.uuid.equals(oppfolgingsperiode) }
	}
}
