package no.nav.veilarbaktivitet.aktivitet

import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.AVTALT
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.AVTALT_DATO_ENDRET
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.BLE_HISTORISK
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.DEL_CV_SVART
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.FATT_JOBBEN
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.FORHAANDSORIENTERING_LEST
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.IKKE_FATT_JOBBEN
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.MOTE_KANAL_ENDRET
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.MOTE_STED_ENDRET
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.MOTE_TIDSPUNKT_ENDRET
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.OPPRETTET
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.REFERAT_ENDRET
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.REFERAT_OPPRETTET
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.REFERAT_PUBLISERT
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.SOKNADSSTATUS_ENDRET
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.STATUS_ENDRET
import no.nav.veilarbaktivitet.aktivitet.AktivitetendringsType.STILLINGSOK_ETIKETT_ENDRET
import no.nav.veilarbaktivitet.aktivitet.Målgruppe.*
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.stilling_fra_nav.Soknadsstatus
import no.nav.veilarbaktivitet.util.DateUtils
import no.nav.veilarbaktivitet.util.DateUtils.norskDato
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class HistorikkService(
    private val aktivitetDAO: AktivitetDAO
) {

    fun hentHistorikk(aktivitetIder: List<AktivitetId>): Map<AktivitetId, Historikk> {
        if (aktivitetIder.isEmpty()) return emptyMap()

        val aktivitetVersjoner: Map<AktivitetId, List<AktivitetData>> =
            aktivitetDAO.hentAktivitetVersjoner(aktivitetIder)
        return lagHistorikkForAktiviteter(aktivitetVersjoner)
    }

}

fun lagHistorikkForAktiviteter(aktivitetVersjoner: Map<AktivitetId, List<AktivitetData>>): Map<AktivitetId, Historikk> {
    return aktivitetVersjoner.map { (aktivitetId, aktivitetVersjoner) ->
        val sorterteAktivitetVersjoner = aktivitetVersjoner.sortedBy { it.versjon }
        val endringer = sorterteAktivitetVersjoner.mapIndexed { index, aktivitetData ->
            Endring(
                endretAvType = aktivitetData.endretAvType,
                endretAv = if (aktivitetData.endretAvType == Innsender.ARBEIDSGIVER) "Arbeidsgiver" else aktivitetData.endretAv,
                tidspunkt = DateUtils.dateToZonedDateTime(aktivitetData.endretDato),
                beskrivelseForVeileder = hentEndringstekst(
                    sorterteAktivitetVersjoner.getOrNull(index - 1),
                    aktivitetData,
                    VEILEDER
                ),
                beskrivelseForBruker = hentEndringstekst(
                    sorterteAktivitetVersjoner.getOrNull(index - 1),
                    aktivitetData,
                    BRUKER
                ),
                beskrivelseForArkiv = hentEndringstekst(
                    sorterteAktivitetVersjoner.getOrNull(index - 1),
                    aktivitetData,
                    ARKIV
                ),
            )
        }
        val endringerSortertMedNyesteEndringFørst = endringer.sortedByDescending { it.tidspunkt }
        aktivitetId to Historikk(endringer = endringerSortertMedNyesteEndringFørst)
    }.toMap()
}

private fun hentEndringstekst(
    forrigeVersjon: AktivitetData?,
    oppdatertVersjon: AktivitetData,
    målgruppe: Målgruppe
): String {
    val endretAvTekst = when (målgruppe) {
        VEILEDER -> endretAvTekstTilVeileder(oppdatertVersjon.endretAvType, oppdatertVersjon.endretAv)
        BRUKER -> endretAvTekstTilBruker(oppdatertVersjon.endretAvType)
        ARKIV -> endretAvTekstTilArkiv(oppdatertVersjon.endretAvType, oppdatertVersjon.endretAv)
    }

    return when (oppdatertVersjon.transaksjonsType) {
        AktivitetTransaksjonsType.OPPRETTET -> {
            if (oppdatertVersjon.isAvtalt)
                "$endretAvTekst opprettet aktiviteten. Den er automatisk merket som \"Avtalt med NAV\""
            else
                "$endretAvTekst opprettet aktiviteten"
        }

        AktivitetTransaksjonsType.STATUS_ENDRET -> "$endretAvTekst flyttet aktiviteten fra ${forrigeVersjon?.status?.text} til ${oppdatertVersjon.status?.text}"
        AktivitetTransaksjonsType.DETALJER_ENDRET -> "$endretAvTekst endret detaljer på aktiviteten"
        AktivitetTransaksjonsType.AVTALT -> {
            if (forrigeVersjon?.isAvtalt ?: false)
                "$endretAvTekst sendte forhåndsorientering"
            else
                "$endretAvTekst merket aktiviteten som \"Avtalt med NAV\""
        }

        AktivitetTransaksjonsType.AVTALT_DATO_ENDRET -> "$endretAvTekst endret til dato på aktiviteten fra ${
            if (forrigeVersjon?.tilDato !== null) norskDato(
                forrigeVersjon.tilDato
            ) else "ingen dato"
        } til ${norskDato(oppdatertVersjon.tilDato)}"

        AktivitetTransaksjonsType.ETIKETT_ENDRET -> {
            val nyEtikett = oppdatertVersjon.stillingsSoekAktivitetData.stillingsoekEtikett?.text ?: "Ingen"
            "$endretAvTekst endret tilstand til $nyEtikett"
        }

        AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET -> "$endretAvTekst endret tid eller sted for møtet"
        AktivitetTransaksjonsType.REFERAT_OPPRETTET -> "$endretAvTekst opprettet referat"
        AktivitetTransaksjonsType.REFERAT_ENDRET -> "$endretAvTekst endret referatet"
        AktivitetTransaksjonsType.REFERAT_PUBLISERT -> "$endretAvTekst delte referatet"
        AktivitetTransaksjonsType.BLE_HISTORISK -> "Aktiviteten ble automatisk arkivert"
        AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST -> {
            val sittEllerDitt = if (målgruppe == BRUKER) "ditt" else "sitt"
            "$endretAvTekst bekreftet å ha lest informasjon om ansvaret $sittEllerDitt"
        }

        AktivitetTransaksjonsType.DEL_CV_SVART -> {
            val svar = if (oppdatertVersjon.stillingFraNavData?.cvKanDelesData?.kanDeles ?: false) "Ja" else "Nei"
            "$endretAvTekst svarte '$svar' på spørsmålet \"Er du interessert i denne stillingen?\""
        }

        AktivitetTransaksjonsType.SOKNADSSTATUS_ENDRET -> {
            val status =
                if (oppdatertVersjon.stillingFraNavData?.soknadsstatus !== null) oppdatertVersjon.stillingFraNavData.soknadsstatus.text else "Ingen"
            "$endretAvTekst endret tilstand til $status"
        }

        AktivitetTransaksjonsType.IKKE_FATT_JOBBEN, AktivitetTransaksjonsType.FATT_JOBBEN -> "$endretAvTekst avsluttet aktiviteten fordi kandidaten har ${oppdatertVersjon.stillingFraNavData.soknadsstatus.text}"
        AktivitetTransaksjonsType.KASSERT -> "$endretAvTekst kasserte aktiviteten"
    }
}

fun endretAvTekstTilArkiv(innsender: Innsender, endretAv: String?) = when (innsender) {
    Innsender.BRUKER -> "Bruker"
    Innsender.ARBEIDSGIVER -> "Arbeidsgiver"
    Innsender.TILTAKSARRANGOER -> "Tiltaksarrangør${endretAv?.let { " $it" } ?: ""}"
    Innsender.NAV, Innsender.ARENAIDENT -> "NAV"
    Innsender.SYSTEM -> "NAV"
}

fun endretAvTekstTilVeileder(innsender: Innsender, endretAv: String?) = when (innsender) {
    Innsender.BRUKER -> "Bruker"
    Innsender.ARBEIDSGIVER -> "Arbeidsgiver"
    Innsender.TILTAKSARRANGOER -> "Tiltaksarrangør${endretAv?.let { " $it" } ?: ""}"
    Innsender.NAV, Innsender.ARENAIDENT -> endretAv?.let { "$endretAv" } ?: "NAV"
    Innsender.SYSTEM -> "NAV"
}

fun endretAvTekstTilBruker(innsender: Innsender) = when (innsender) {
    Innsender.BRUKER -> "Du"
    Innsender.ARBEIDSGIVER -> "Arbeidsgiver"
    Innsender.TILTAKSARRANGOER -> "Tiltaksarrangør"
    Innsender.NAV, Innsender.ARENAIDENT, Innsender.SYSTEM -> "NAV"
}

typealias AktivitetId = Long

data class Historikk(
    val endringer: List<Endring>
)

data class Endring(
    val endretAvType: Innsender,
    val endretAv: String?,
    val tidspunkt: ZonedDateTime,
    val beskrivelseForVeileder: String,
    val beskrivelseForBruker: String,
    val beskrivelseForArkiv: String,
)

private enum class Målgruppe {
    VEILEDER,
    BRUKER,
    ARKIV
}

fun utledAktivitetendringsType(forrigeVersjon: AktivitetData?, oppdatertVersjon: AktivitetData): List<AktivitetendringsType> {
    // Endringer som bare oppstår alene
    if (forrigeVersjon == null) return listOf(OPPRETTET)
    if (forrigeVersjon.historiskDato == null && oppdatertVersjon.historiskDato != null) return listOf(BLE_HISTORISK)

    val endringer = mutableListOf<AktivitetendringsType>()
    if (forrigeVersjon.status != oppdatertVersjon.status) endringer.add(STATUS_ENDRET)
    if (!forrigeVersjon.isAvtalt && oppdatertVersjon.isAvtalt) endringer.add(AVTALT)
    if (oppdatertVersjon.aktivitetType != AktivitetTypeData.MOTE && forrigeVersjon.tilDato != oppdatertVersjon.tilDato)
        endringer.add(AVTALT_DATO_ENDRET)
    if (erMøtetidspunktEndret(forrigeVersjon, oppdatertVersjon)) endringer.add(MOTE_TIDSPUNKT_ENDRET)
    if (erMøtestedEndret(forrigeVersjon, oppdatertVersjon)) endringer.add(MOTE_STED_ENDRET)
    if (erMøtekanalEndret(forrigeVersjon, oppdatertVersjon)) endringer.add(MOTE_KANAL_ENDRET)
    if (erReferatOpprettet(forrigeVersjon, oppdatertVersjon)) endringer.add(REFERAT_OPPRETTET)
    if (erReferatEndret(forrigeVersjon, oppdatertVersjon)) endringer.add(REFERAT_ENDRET)
    if (erReferatPublisert(forrigeVersjon, oppdatertVersjon)) endringer.add(REFERAT_PUBLISERT)
    if (erStillingsokEtikettEndret(forrigeVersjon, oppdatertVersjon)) endringer.add(STILLINGSOK_ETIKETT_ENDRET)
    if (erForhaandsorienteringBlittLest(forrigeVersjon, oppdatertVersjon)) endringer.add(FORHAANDSORIENTERING_LEST)
    if (harCvDeltBlittBesvart(forrigeVersjon, oppdatertVersjon)) endringer.add(DEL_CV_SVART)
    if (erSøknadsstatusEndret(forrigeVersjon, oppdatertVersjon)) endringer.add(SOKNADSSTATUS_ENDRET)
    if (erEndretTilIkkeFåttJobben(forrigeVersjon, oppdatertVersjon)) endringer.add(IKKE_FATT_JOBBEN)
    if (erEndretTilFåttJobben(forrigeVersjon, oppdatertVersjon)) endringer.add(FATT_JOBBEN)
    // TODO: Detaljer endret
}

private fun erMøtetidspunktEndret(forrigeVersjon: AktivitetData, oppdatertVersjon: AktivitetData): Boolean {
    if (forrigeVersjon.aktivitetType != AktivitetTypeData.MOTE) return false
    val startTidspunktEndret = forrigeVersjon.fraDato != oppdatertVersjon.fraDato
    val sluttTidspunktEndret = forrigeVersjon.tilDato != forrigeVersjon.tilDato
    return startTidspunktEndret || sluttTidspunktEndret
}

private fun erMøtestedEndret(forrigeVersjon: AktivitetData, oppdatertVersjon: AktivitetData): Boolean {
    if (forrigeVersjon.aktivitetType != AktivitetTypeData.MOTE) return false
    return forrigeVersjon.moteData.adresse != oppdatertVersjon.moteData.adresse
}

private fun erMøtekanalEndret(forrigeVersjon: AktivitetData, oppdatertVersjon: AktivitetData): Boolean {
    if (forrigeVersjon.aktivitetType != AktivitetTypeData.MOTE) return false
    return forrigeVersjon.moteData.kanal != oppdatertVersjon.moteData.kanal
}

private fun erReferatOpprettet(forrigeVersjon: AktivitetData, oppdatertVersjon: AktivitetData): Boolean {
    if (forrigeVersjon.aktivitetType != AktivitetTypeData.MOTE || forrigeVersjon.aktivitetType != AktivitetTypeData.SAMTALEREFERAT) return false
    return forrigeVersjon.moteData.referat.isNullOrEmpty() && !oppdatertVersjon.moteData.referat.isNullOrEmpty()
}

private fun erReferatEndret(forrigeVersjon: AktivitetData, oppdatertVersjon: AktivitetData): Boolean {
    if (forrigeVersjon.aktivitetType != AktivitetTypeData.MOTE || forrigeVersjon.aktivitetType != AktivitetTypeData.SAMTALEREFERAT) return false
    return !forrigeVersjon.moteData.referat.isNullOrEmpty() &&
            (forrigeVersjon.moteData.referat != oppdatertVersjon.moteData.referat)
}

private fun erReferatPublisert(forrigeVersjon: AktivitetData, oppdatertVersjon: AktivitetData): Boolean {
    if (forrigeVersjon.aktivitetType != AktivitetTypeData.MOTE || forrigeVersjon.aktivitetType != AktivitetTypeData.SAMTALEREFERAT) return false
    return !forrigeVersjon.moteData.isReferatPublisert && oppdatertVersjon.moteData.isReferatPublisert
}

private fun erStillingsokEtikettEndret(forrigeVersjon: AktivitetData, oppdatertVersjon: AktivitetData): Boolean {
    // TODO: Hvilken type er dette aktuelt for?
    return forrigeVersjon.stillingsSoekAktivitetData?.stillingsoekEtikett != oppdatertVersjon.stillingsSoekAktivitetData?.stillingsoekEtikett
}

private fun erForhaandsorienteringBlittLest(forrigeVersjon: AktivitetData, oppdatertVersjon: AktivitetData): Boolean {
    if (forrigeVersjon.forhaandsorientering == null) return false
    return forrigeVersjon.forhaandsorientering.lestDato == null && forrigeVersjon.forhaandsorientering.lestDato != null
}

private fun harCvDeltBlittBesvart(forrigeVersjon: AktivitetData, oppdatertVersjon: AktivitetData): Boolean {
    if (forrigeVersjon.stillingFraNavData?.cvKanDelesData == null) return false
    return forrigeVersjon.stillingFraNavData.cvKanDelesData.kanDeles == null && oppdatertVersjon.stillingFraNavData.cvKanDelesData.kanDeles != null
}

private fun erSøknadsstatusEndret(forrigeVersjon: AktivitetData, oppdatertVersjon: AktivitetData): Boolean {
    if (forrigeVersjon.stillingFraNavData?.soknadsstatus == null) return false
    return forrigeVersjon.stillingFraNavData.soknadsstatus != oppdatertVersjon.stillingFraNavData?.soknadsstatus
}

private fun erEndretTilIkkeFåttJobben(forrigeVersjon: AktivitetData, oppdatertVersjon: AktivitetData): Boolean {
    if (forrigeVersjon.stillingFraNavData == null) return false
    return forrigeVersjon.stillingFraNavData?.soknadsstatus != Soknadsstatus.IKKE_FATT_JOBBEN && oppdatertVersjon.stillingFraNavData?.soknadsstatus == Soknadsstatus.IKKE_FATT_JOBBEN
}

private fun erEndretTilFåttJobben(forrigeVersjon: AktivitetData, oppdatertVersjon: AktivitetData): Boolean {
    if (forrigeVersjon.stillingFraNavData == null) return false
    return forrigeVersjon.stillingFraNavData?.soknadsstatus != Soknadsstatus.FATT_JOBBEN && oppdatertVersjon.stillingFraNavData?.soknadsstatus == Soknadsstatus.FATT_JOBBEN
}


enum class AktivitetendringsType {
    // Endringer som opptrer alene
    BLE_HISTORISK, // OK
    OPPRETTET, // OK
    KASSERT,

    // Endringer som kan opptre sammen med andre endringer
    STATUS_ENDRET, // OK
    AVTALT, // OK
    AVTALT_DATO_ENDRET, // OK
    STILLINGSOK_ETIKETT_ENDRET, // OK
    MOTE_TIDSPUNKT_ENDRET, // OK
    MOTE_STED_ENDRET, // OK
    MOTE_KANAL_ENDRET, // OK
    REFERAT_OPPRETTET, // OK
    REFERAT_ENDRET, // OK
    REFERAT_PUBLISERT, // OK
    FORHAANDSORIENTERING_LEST, // OK
    DEL_CV_SVART, // OK
    SOKNADSSTATUS_ENDRET, // OK
    IKKE_FATT_JOBBEN, // OK
    FATT_JOBBEN, // OK
    DETALJER_ENDRET,
}