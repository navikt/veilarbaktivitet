package no.nav.veilarbaktivitet.aktivitet.domain

import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.ReferatEndring
import no.nav.veilarbaktivitet.eventsLogger.EventType
import java.util.HashSet

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

fun hentEventTypeForReferat(
    orginalMoteData: MoteData,
    oppdatertAktivitet: ReferatEndring
): EventType? {
    val forrigeReferat = orginalMoteData.referat ?: ""
    val nesteReferat = oppdatertAktivitet.moteData.referat ?: ""

    val referatHarNåFåttInnhold = forrigeReferat.isEmpty() && nesteReferat.isNotEmpty()
    val referatHarNåBlittDeltMedBruker =
        !orginalMoteData.isReferatPublisert && oppdatertAktivitet.moteData.isReferatPublisert
    val referatVarAlleredeDelt = orginalMoteData.isReferatPublisert

    return if (referatHarNåFåttInnhold && !referatHarNåBlittDeltMedBruker && !referatVarAlleredeDelt) {
        EventType.SAMTALEREFERAT_OPPRETTET
    } else if (referatHarNåFåttInnhold && referatHarNåBlittDeltMedBruker) {
        EventType.SAMTALEREFERAT_OPPRETTET_OG_DELT_MED_BRUKER
    } else if (!referatHarNåFåttInnhold && referatHarNåBlittDeltMedBruker) {
        EventType.SAMTALEREFERAT_DELT_MED_BRUKER
    } else if (referatHarNåFåttInnhold && referatVarAlleredeDelt) {
        EventType.SAMTALEREFERAT_FIKK_INNHOLD
    } else {
        null
    }
}

val TYPER_SOM_KAN_ENDRES_EKSTERNT: Set<AktivitetTypeData> = HashSet(
    listOf(
        AktivitetTypeData.EGENAKTIVITET,
        AktivitetTypeData.JOBBSOEKING,
        AktivitetTypeData.SOKEAVTALE,
        AktivitetTypeData.IJOBB,
        AktivitetTypeData.BEHANDLING,
        AktivitetTypeData.STILLING_FRA_NAV
    )
)

val TYPER_SOM_KAN_OPPRETTES_EKSTERNT: MutableSet<AktivitetTypeData?> = HashSet(
    listOf(
        AktivitetTypeData.BEHANDLING,
        AktivitetTypeData.EGENAKTIVITET,
        AktivitetTypeData.JOBBSOEKING,
        AktivitetTypeData.IJOBB
    )
)