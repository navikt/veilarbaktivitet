package no.nav.veilarbaktivitet.oppfolging.periode

import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.brukernotifikasjon.MinsideVarselService
import no.nav.veilarbaktivitet.oppfolging.client.MålDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingClient
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.oppfolging.client.SakDTO
import no.nav.veilarbaktivitet.oversikten.OversiktenService
import no.nav.veilarbaktivitet.person.Person.AktorId
import no.nav.veilarbaktivitet.person.Person.Fnr
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

@Service
class OppfolgingsperiodeService(
    private val aktivitetService: AktivitetService,
    private val minsideVarselService: MinsideVarselService,
    private val sistePeriodeDAO: SistePeriodeDAO,
    private val oppfolgingsperiodeDAO: OppfolgingsperiodeDAO,
    private val oppfolgingClient: OppfolgingClient,
    private val oversiktenService: OversiktenService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        val SLACK_FOER: Duration = Duration.ofDays(7)
    }

    fun avsluttOppfolgingsperiode(oppfolgingsperiode: UUID, sluttDato: ZonedDateTime) {
        log.info("avsluttOppfolgingsperiode: {}", oppfolgingsperiode)
        minsideVarselService.setDoneGrupperingsID(oppfolgingsperiode)
        aktivitetService.settAktiviteterTilHistoriske(oppfolgingsperiode, sluttDato)
        oversiktenService.lagreStoppMeldingVedAvsluttOppfolging(oppfolgingsperiode)
    }

    fun upsertOppfolgingsperiode(sisteOppfolgingsperiodeV1: SisteOppfolgingsperiodeV1) {
        val oppfolgingsperiode = Oppfolgingsperiode(
            sisteOppfolgingsperiodeV1.aktorId,
            sisteOppfolgingsperiodeV1.uuid,
            sisteOppfolgingsperiodeV1.startDato,
            sisteOppfolgingsperiodeV1.sluttDato
        )
        oppfolgingsperiodeDAO.upsertOppfolgingsperide(oppfolgingsperiode)
        sistePeriodeDAO.uppsertOppfolingsperide(oppfolgingsperiode)
    }

    fun hentOppfolgingsperiode(aktorId: AktorId, oppfolgingsperiode: UUID): OppfolgingPeriodeMinimalDTO? {
        return oppfolgingClient.hentOppfolgingsperioder(aktorId).find { it.uuid.equals(oppfolgingsperiode) }
    }

    fun hentOppfolgingsperiode(oppfolgingsperiodeId: UUID): Oppfolgingsperiode? {
        return oppfolgingsperiodeDAO.getOppfolgingsperiode(oppfolgingsperiodeId)
    }

    fun hentSak(oppfolgingsperiodeId: UUID): SakDTO? {
        return oppfolgingClient.hentSak(oppfolgingsperiodeId).orElseGet(null)
    }

    fun hentMål(fnr: Fnr): MålDTO {
        return oppfolgingClient.hentMål(fnr).orElseGet(null)
    }

    fun hentOppfolgingsPerioder(aktorId: AktorId): List<Oppfolgingsperiode> {
        return oppfolgingsperiodeDAO.getByAktorId(aktorId)
    }
}
