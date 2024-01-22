package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.arkivering.ArkivAktivitet
import no.nav.veilarbaktivitet.arkivering.Detalj
import no.nav.veilarbaktivitet.arkivering.Stil
import no.nav.veilarbaktivitet.arkivering.Stil.PARAGRAF
import no.nav.veilarbaktivitet.internapi.model.Mote.KanalEnum
import no.nav.veilarbaktivitet.util.DateUtils
import no.nav.veilarbaktivitet.util.DateUtils.*
import java.text.DateFormat
import java.time.format.DateTimeFormatter
import java.util.*

fun AktivitetData.toArkivPayload(): ArkivAktivitet {
    return ArkivAktivitet(
        tittel = this.tittel,
        type = this.toArkivTypeTekst(),
        status = this.status.toArkivTekst(),
        detaljer = emptyList(),
        tags = emptyList()
    )
}

fun AktivitetStatus.toArkivTekst(): String {
    return when (this) {
        AktivitetStatus.AVBRUTT -> "Avbrutt"
        AktivitetStatus.BRUKER_ER_INTERESSERT -> "Forslag"
        AktivitetStatus.FULLFORT -> "Fullført"
        AktivitetStatus.GJENNOMFORES -> "Gjennomføres"
        AktivitetStatus.PLANLAGT -> "Planlagt"
        else -> throw IllegalArgumentException("Fant ingen status")
    }
}

fun AktivitetData.toArkivTypeTekst(): String {
    return when (this.aktivitetType) {
        AktivitetTypeData.EGENAKTIVITET -> "Jobbrettet egenaktivitet"
        AktivitetTypeData.JOBBSOEKING -> "Stilling"
        AktivitetTypeData.SOKEAVTALE -> "Jobbsøking"
        AktivitetTypeData.IJOBB -> "Jobb jeg har nå"
        AktivitetTypeData.BEHANDLING -> "Behandling"
        AktivitetTypeData.MOTE -> "Møte med NAV"
        AktivitetTypeData.SAMTALEREFERAT -> "Samtalereferat"
        AktivitetTypeData.STILLING_FRA_NAV -> "Stilling fra NAV"
        AktivitetTypeData.EKSTERNAKTIVITET -> {
            when (this.eksternAktivitetData.type!!) {
                AktivitetskortType.ARENA_TILTAK -> "Tiltak gjennom NAV"
                AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD -> "Avtale midlertidig lønnstilskudd"
                AktivitetskortType.VARIG_LONNSTILSKUDD -> "Avtale varig lønnstilskudd"
                AktivitetskortType.INDOPPFAG -> "Oppfølging"
                AktivitetskortType.ARBFORB -> "Arbeidsforberedende trening"
                AktivitetskortType.AVKLARAG -> "Avklaring"
                AktivitetskortType.VASV -> "Tilrettelagt arbeid"
                AktivitetskortType.ARBRRHDAG -> "Arbeidsrettet rehabilitering"
                AktivitetskortType.DIGIOPPARB -> "Digitalt oppfølgingstiltak"
                AktivitetskortType.JOBBK -> "Jobbklubb"
                AktivitetskortType.GRUPPEAMO -> "Arbeidsmarkedsopplæring (Gruppe)"
                AktivitetskortType.GRUFAGYRKE -> "Fag- og yrkesopplæring (Gruppe)"
            }
        }
        /* Arena aktiviteter - Ingen av disse er lagret hos oss, da er de Ekstern-aktivtietr
        TILTAKSAKTIVITET -> 'Tiltak gjennom NAV',
        GRUPPEAKTIVITET -> 'Gruppeaktivitet',
        UTDANNINGSAKTIVITET -> 'Utdanning',
        */
    }
}

fun AktivitetData.toDetaljer(): List<Detalj> {
    return listOf(
        Detalj(stil = Stil.HALV_LINJE, tittel = "Fra dato", tekst = dateToZonedDateTime(fraDato).format(datoFormat)),
        Detalj(stil = Stil.HALV_LINJE, tittel = "Til dato", tekst = dateToZonedDateTime(tilDato).format(datoFormat)), // TODO: Trenger klokkeslett og varighet på møte med nav
        Detalj(stil = Stil.HALV_LINJE, tittel = "Møteform", tekst = this.moteData.kanal.tekst),
        Detalj(stil = Stil.HEL_LINJE, tittel = "Møtested eller annen praktisk informasjon", tekst = this.moteData.adresse),
        Detalj(stil = Stil.HEL_LINJE, tittel = "Hensikt med møtet", tekst = this.beskrivelse),
        Detalj(stil = Stil.HEL_LINJE, tittel = "Forberedelser til møtet", tekst = this.moteData.forberedelser),

    )
}

private val datoFormat = DateTimeFormatter.ofPattern("dd MMMM uuuu")