package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.arkivering.AktivitetEndring
import no.nav.veilarbaktivitet.arkivering.AktivitetHistorikk
import no.nav.veilarbaktivitet.util.DateUtils.norskDatoOgKlokkeslett

fun Historikk.tilAktivitetHistorikk(): AktivitetHistorikk {
    return AktivitetHistorikk(
        endringer = this.endringer.map { AktivitetEndring(
            formattertTidspunkt = norskDatoOgKlokkeslett(it.tidspunkt),
            beskrivelse = it.beskrivelseForArkiv
        ) }
    )
}