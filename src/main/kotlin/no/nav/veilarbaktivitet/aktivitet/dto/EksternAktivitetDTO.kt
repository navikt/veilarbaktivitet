package no.nav.veilarbaktivitet.aktivitet.dto

import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.LenkeSeksjon
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Oppgaver

data class EksternAktivitetDTO(
    val type: AktivitetskortType,
    val oppgave: Oppgaver?,
    val handlinger: List<LenkeSeksjon>?,
    val detaljer: List<Attributt>?,
    val etiketter: List<Etikett>?
)
