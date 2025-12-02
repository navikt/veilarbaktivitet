package no.nav.veilarbaktivitet.arkivering

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
    val aktiviteter = when (filter.avtaltMedNav) {
        ArkiveringsController.AvtaltMedNav.AVTALT_MED_NAV -> this.aktiviteter.filter { it.isAvtalt }
        ArkiveringsController.AvtaltMedNav.IKKE_AVTALT_MED_NAV -> this.aktiviteter.filter { !it.isAvtalt}
        null -> this.aktiviteter
    }
    return this.copy(aktiviteter = aktiviteter)
}
