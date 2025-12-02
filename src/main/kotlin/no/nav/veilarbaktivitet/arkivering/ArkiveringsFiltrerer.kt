package no.nav.veilarbaktivitet.arkivering

fun filtrerArkiveringsData(arkiveringsData: ArkiveringsController.ArkiveringsData, filter: ArkiveringsController.Filter): ArkiveringsController.ArkiveringsData {
    val arkiveringsDataEtterFiltreringPåHistorikk = if(!filter.inkluderHistorikk) {
        arkiveringsData.copy(historikkForAktiviteter = emptyMap())
    } else {
        arkiveringsData
    }

    return arkiveringsDataEtterFiltreringPåHistorikk
}