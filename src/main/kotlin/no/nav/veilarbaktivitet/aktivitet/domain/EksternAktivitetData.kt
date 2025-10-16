package no.nav.veilarbaktivitet.aktivitet.domain

import lombok.Singular
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.LenkeSeksjon
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Oppgaver
import no.nav.veilarbaktivitet.arena.model.ArenaId
import java.time.LocalDateTime
import java.time.ZonedDateTime

data class EksternAktivitetData(
    val source: String? = null,
    val tiltaksKode: String? = null,
    val opprettetSomHistorisk: Boolean = false,
    val oppfolgingsperiodeSlutt: LocalDateTime? = null,
    val arenaId: ArenaId? = null,
    val type: AktivitetskortType,
    val oppgave: Oppgaver? = null,
    @Singular("handling")
    val handlinger: List<LenkeSeksjon>? = null,
    @Singular("detalj")
    val detaljer: List<Attributt>? = null,
    @Singular("etikett")
    val etiketter: List<Etikett>? = null,
    val endretTidspunktKilde: ZonedDateTime? = null
) {}
