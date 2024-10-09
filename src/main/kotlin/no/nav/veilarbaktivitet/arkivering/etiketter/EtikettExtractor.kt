package no.nav.veilarbaktivitet.arkivering.etiketter

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("EtikettExtractor.kt")

fun Etikett.mapTilArenaEtikett(): ArkivEtikett? {
    return when (this.kode) {
        "SOKT_INN" -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Søkt inn på tiltaket")
        "AVSLAG" -> ArkivEtikett(ArkivEtikettStil.NEGATIVE, "Fått avslag")
        "IKKE_AKTUELL" -> ArkivEtikett(ArkivEtikettStil.NEUTRAL, "Ikke aktuell for tiltaket")
        "IKKE_MOETT" -> ArkivEtikett(ArkivEtikettStil.NEGATIVE, "Ikke møtt på tiltaket")
        "INFOMOETE" -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Infomøte før tiltaket")
        "TAKKET_JA" -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Takket ja til tilbud")
        "TAKKET_NEI" -> ArkivEtikett(ArkivEtikettStil.NEUTRAL, "Takket nei til tilbud")
        "FATT_PLASS" -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Fått plass på tiltaket")
        "VENTELISTE" -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "På venteliste")
        "SOKNAD_SENDT" -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Søknaden er sendt")
        "INNKALT_TIL_INTERVJU" -> ArkivEtikett(ArkivEtikettStil.NEUTRAL, "Skal på intervju")
        "JOBBTILBUD" -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Fått jobbtilbud")
        else -> {
            logger.error("Fant ukjent arena-etikettkode")
            return null
        }
    }
}

private fun AktivitetData.getTypeEtiketter(): List<ArkivEtikett> {
    return when (this.aktivitetType) {
        AktivitetTypeData.JOBBSOEKING -> this.stillingsSoekAktivitetData?.stillingsoekEtikett
            ?.text?.let { ArkivEtikett(ArkivEtikettStil.POSITIVE, it) }.wrapInList()

        AktivitetTypeData.STILLING_FRA_NAV -> this.stillingFraNavData.soknadsstatus
            ?.text?.let { ArkivEtikett(ArkivEtikettStil.POSITIVE, it) }.wrapInList()

        AktivitetTypeData.EKSTERNAKTIVITET -> {
            if (this.eksternAktivitetData.type == AktivitetskortType.ARENA_TILTAK) {
                eksternAktivitetData.etiketter?.mapNotNull {
                    it.mapTilArenaEtikett()
                } ?: emptyList()
            } else {
            this.eksternAktivitetData?.etiketter?.map {
                ArkivEtikett(it.sentiment.toArkivEtikettStil(), it.tekst)
            } ?: emptyList() }
        }
        else -> emptyList()
    }
}

private fun AktivitetData.avtaltEtikett(): ArkivEtikett? {
    return if (this.isAvtalt) ArkivEtikett(ArkivEtikettStil.AVTALT, "Avtalt med NAV") else null
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
