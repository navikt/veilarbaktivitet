package no.nav.veilarbaktivitet.arkivering.etiketter

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData

class EtikettExtractor {
    private fun getStilllingEtikett(aktivitet: AktivitetData): List<ArkivEtikett> {
        return when (aktivitet.aktivitetType) {
            AktivitetTypeData.JOBBSOEKING -> aktivitet.stillingsSoekAktivitetData?.stillingsoekEtikett
                ?.toArkivEtikettString()?.let { ArkivEtikett(ArkivEtikettStil.POSITIVE, it) }.wrapInList()
            AktivitetTypeData.STILLING_FRA_NAV -> aktivitet.stillingFraNavData.soknadsstatus
                ?.toArkivEtikettString()?.let { ArkivEtikett(ArkivEtikettStil.POSITIVE, it) }.wrapInList()
            AktivitetTypeData.EKSTERNAKTIVITET -> aktivitet.eksternAktivitetData?.etiketter?.map {
                ArkivEtikett(it.sentiment.toArkivEtikettStil(), it.tekst) } ?: emptyList()
            else -> emptyList()
        }
    }

    fun ArkivEtikett?.wrapInList(): List<ArkivEtikett> {
        return this?.let { listOf(it) } ?: emptyList()
    }

    fun getFilterTags(AktivitetData aktivitet): List<ArkivEtikett> {
        return Filters.listOf(
            Filters.of("status", aktivitet.getStatus().toString()),
            Filters.of("aktivitetsType", aktivitet.getAktivitetType().toString()),
            Filters.of("avtaltAktivitet", aktivitet.isAvtalt()),
            getStilllingEtikett(aktivitet)
        );
    }

//    public static List<FilterTag> getFilterTags(ArenaAktivitetDTO aktivitet) {
//        return Filters.listOf(
//            Filters.of("status", aktivitet.getStatus().toString()),
//            Filters.of("aktivitetsType", aktivitet.getType().toString()),
//            Filters.of("avtaltAktivitet", aktivitet.isAvtalt()),
//            Filters.of("tiltakstatus", aktivitet.getEtikett().toString())
//        );
//    }
}