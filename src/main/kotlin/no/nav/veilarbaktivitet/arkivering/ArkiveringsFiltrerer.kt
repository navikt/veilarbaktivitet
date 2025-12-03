package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData

fun filtrerArkiveringsData(arkiveringsData: ArkiveringsController.ArkiveringsData, filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    return arkiveringsData
        .filtrerPåHistorikk(filter)
        .filtrerPåAvtaltMedNavn(filter)
}

private fun ArkiveringsController.ArkiveringsData.filtrerPåHistorikk(filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    return if(!filter.inkluderHistorikk) {
        this.copy(historikkForAktiviteter = emptyMap())
    } else {
        this
    }
}

private fun ArkiveringsController.ArkiveringsData.filtrerPåAvtaltMedNavn(filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    if (filter.aktivitetAvtaltMedNav.isEmpty()) return this

    val predikater = filter.aktivitetAvtaltMedNav.map { filter ->
        when (filter) {
            ArkiveringsController.AvtaltMedNav.AVTALT_MED_NAV -> { aktivitetData: AktivitetData -> aktivitetData.isAvtalt }
            ArkiveringsController.AvtaltMedNav.IKKE_AVTALT_MED_NAV -> { aktivitetData: AktivitetData -> !aktivitetData.isAvtalt }
        }
    }
    val filtrerteAktiviteter = this.aktiviteter.filter { aktivitetData ->
        predikater.any { it(aktivitetData) }
    }
    return this.copy(aktiviteter = filtrerteAktiviteter)
}
