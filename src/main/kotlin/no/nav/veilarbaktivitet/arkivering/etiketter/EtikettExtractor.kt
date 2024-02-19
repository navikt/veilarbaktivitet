package no.nav.veilarbaktivitet.arkivering.etiketter

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData


private fun AktivitetData.getTypeEtiketter(): List<ArkivEtikett> {
    return when (this.aktivitetType) {
        AktivitetTypeData.JOBBSOEKING -> this.stillingsSoekAktivitetData?.stillingsoekEtikett
            ?.text?.let { ArkivEtikett(ArkivEtikettStil.POSITIVE, it) }.wrapInList()
        AktivitetTypeData.STILLING_FRA_NAV -> this.stillingFraNavData.soknadsstatus
            ?.text?.let { ArkivEtikett(ArkivEtikettStil.POSITIVE, it) }.wrapInList()
        AktivitetTypeData.EKSTERNAKTIVITET -> this.eksternAktivitetData?.etiketter?.map {
            ArkivEtikett(it.sentiment.toArkivEtikettStil(), it.tekst) } ?: emptyList()
        else -> emptyList()
    }
}

private fun AktivitetData.avtaltEtikett(): ArkivEtikett? {
    return if (this.isAvtalt) ArkivEtikett(ArkivEtikettStil.AVTALT , "Avtalt med NAV") else null
}

fun AktivitetData.getArkivEtiketter(): List<ArkivEtikett> {
    return listOf(
        getTypeEtiketter(),
        avtaltEtikett().wrapInList()
    ).flatten()
}

// TODO: Arena etiketter kan lages når man arena-aktiviteter er inkludert i jounalførte aktiviteter
//    public static List<FilterTag> getFilterTags(ArenaAktivitetDTO aktivitet) {
//        return Filters.listOf(
//            Filters.of("status", aktivitet.getStatus().toString()),
//            Filters.of("aktivitetsType", aktivitet.getType().toString()),
//            Filters.of("avtaltAktivitet", aktivitet.isAvtalt()),
//            Filters.of("tiltakstatus", aktivitet.getEtikett().toString())
//        );
//    }


fun ArkivEtikett?.wrapInList(): List<ArkivEtikett> {
    return this?.let { listOf(it) } ?: emptyList()
}
