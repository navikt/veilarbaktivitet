package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.arkivering.AktivitetEndring
import no.nav.veilarbaktivitet.arkivering.ArkivAktivitetHistorikk
import no.nav.veilarbaktivitet.util.DateUtils

fun no.nav.veilarbaktivitet.aktivitet.AktivitetHistorikk.tilAktivitetHistorikk(): ArkivAktivitetHistorikk {
    return ArkivAktivitetHistorikk(
        endringer = this.endringer.map { AktivitetEndring(
            formattertTidspunkt = "${DateUtils.norskDato(it.tidspunkt)} kl. ${ DateUtils.klokkeslett(it.tidspunkt) }",
            beskrivelse = it.beskrivelseForArkiv
        ) }
    )
}