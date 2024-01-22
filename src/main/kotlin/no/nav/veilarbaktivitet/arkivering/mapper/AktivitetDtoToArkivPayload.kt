package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.arkivering.ArkivAktivitet

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