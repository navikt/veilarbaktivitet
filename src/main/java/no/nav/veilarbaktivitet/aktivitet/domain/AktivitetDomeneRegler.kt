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