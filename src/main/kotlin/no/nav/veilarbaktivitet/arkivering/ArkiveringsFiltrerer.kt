package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData

fun filtrerArkiveringsData(arkiveringsData: ArkiveringsController.ArkiveringsData, filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    return arkiveringsData
        .filtrerPåHistorikk(filter)
        .filtrerPåAvtaltMedNavn(filter)
        .filtrerPåStillingsstatus(filter)
}

private fun ArkiveringsController.ArkiveringsData.filtrerPåHistorikk(filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    return if(!filter.inkluderHistorikk) {
        this.copy(historikkForAktiviteter = emptyMap())
    } else {
        this
    }
}

private fun ArkiveringsController.ArkiveringsData.filtrerPåAvtaltMedNavn(filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    if (filter.aktivitetAvtaltMedNavFilter.isEmpty()) return this

    val predikater = filter.aktivitetAvtaltMedNavFilter.map { filter ->
        when (filter) {
            ArkiveringsController.AvtaltMedNavFilter.AVTALT_MED_NAV -> { aktivitetData: AktivitetData -> aktivitetData.isAvtalt }
            ArkiveringsController.AvtaltMedNavFilter.IKKE_AVTALT_MED_NAV -> { aktivitetData: AktivitetData -> !aktivitetData.isAvtalt }
        }
    }
    val filtrerteAktiviteter = this.aktiviteter.filter { aktivitetData ->
        predikater.any { it(aktivitetData) }
    }
    return this.copy(aktiviteter = filtrerteAktiviteter)
}

private fun ArkiveringsController.ArkiveringsData.filtrerPåStillingsstatus(filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    if (filter.stillingsstatusFilter.isEmpty()) return this

    val filtrerteAktiviteter = aktiviteter.filter { aktivitet ->
        if (aktivitet.stillingFraNavData == null) true
        else aktivitet.stillingFraNavData.soknadsstatus in filter.stillingsstatusFilter
    }
    return this.copy(aktiviteter = filtrerteAktiviteter)
}