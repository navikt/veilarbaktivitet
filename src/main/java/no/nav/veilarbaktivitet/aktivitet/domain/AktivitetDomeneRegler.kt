package no.nav.veilarbaktivitet.aktivitet.domain

fun nyopprettetAktivitetKanHaReferat(nyAktivitet: AktivitetData): Boolean {
    val aktivitetstyperSomKanHaReferatNårAktivitetOpprettes: List<AktivitetTypeData> =
        listOf(AktivitetTypeData.SAMTALEREFERAT)
    return aktivitetstyperSomKanHaReferatNårAktivitetOpprettes.contains(nyAktivitet.aktivitetType)
}

fun referatErDeltMedBruker(nyAktivitet: AktivitetData): Boolean {
    val moteData = nyAktivitet.moteData
    return moteData?.isReferatPublisert ?: false
}

fun erAktivitetsVersjonSynligForEksterne(aktivitetData: AktivitetData): Boolean {
    return !(erMøteEllerSamtalereferat(aktivitetData) && erReferatetEndretForDetErPublisert(aktivitetData))
}

private fun erReferatetEndretForDetErPublisert(aktivitetData: AktivitetData): Boolean {
    val referatEndret = AktivitetTransaksjonsType.REFERAT_ENDRET == aktivitetData.transaksjonsType ||
            AktivitetTransaksjonsType.REFERAT_OPPRETTET == aktivitetData.transaksjonsType
    return !(aktivitetData.moteData?.isReferatPublisert ?: false) && referatEndret
}

private fun erMøteEllerSamtalereferat
            (aktivitetData: AktivitetData): Boolean {
    return aktivitetData.aktivitetType == AktivitetTypeData.MOTE ||
            aktivitetData.aktivitetType == AktivitetTypeData.SAMTALEREFERAT
}
