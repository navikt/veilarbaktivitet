package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.arkivering.AktivitetEndring
import no.nav.veilarbaktivitet.arkivering.AktivitetHistorikk
import no.nav.veilarbaktivitet.util.DateUtils

fun Historikk.tilAktivitetHistorikk(): AktivitetHistorikk {
    return AktivitetHistorikk(
        endringer = this.endringer.map { AktivitetEndring(
            formattertTidspunkt = "${DateUtils.norskDato(it.tidspunkt)} kl. ${ DateUtils.klokkeslett(it.tidspunkt) }",
            beskrivelseForBruker = it.beskrivelseForBruker,
            beskrivelseForVeileder = it.beskrivelseForVeileder
        ) }
    )
}