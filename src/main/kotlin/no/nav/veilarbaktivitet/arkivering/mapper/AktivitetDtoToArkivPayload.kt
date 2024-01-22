package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.common.utils.EnvironmentUtils.isProduction
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.arkivering.ArkivAktivitet
import no.nav.veilarbaktivitet.arkivering.Detalj
import no.nav.veilarbaktivitet.arkivering.DetaljUtkast
import no.nav.veilarbaktivitet.arkivering.Stil.*
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData
import no.nav.veilarbaktivitet.util.DateUtils.dateToZonedDateTime
import java.time.format.DateTimeFormatter

fun AktivitetData.toArkivPayload(): ArkivAktivitet {
    return ArkivAktivitet(
        tittel = this.tittel,
        type = this.toArkivTypeTekst(),
        status = this.status.toArkivTekst(),
        detaljer = this.toDetaljer(),
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

fun AktivitetData.erMoteEllerSamtaleReferat() = this.aktivitetType in listOf(AktivitetTypeData.MOTE, AktivitetTypeData.SAMTALEREFERAT)
fun AktivitetData.erStilling() = this.aktivitetType in listOf(AktivitetTypeData.JOBBSOEKING)

fun AktivitetData.toDetaljer(): List<Detalj> {
    return listOfNotNull(
        DetaljUtkast(stil = HALV_LINJE, tittel = "Fra dato", tekst = fraDato.let { dateToZonedDateTime(it).format(datoFormat) }),
        if (!erStilling())
            DetaljUtkast(stil = HALV_LINJE, tittel = "Til dato", tekst = tilDato?.let { dateToZonedDateTime(it).format(datoFormat) }) // TODO: Trenger klokkeslett og varighet på møte med nav, skal hete til-data
            else null,
        if(!erMoteEllerSamtaleReferat())
            DetaljUtkast(stil = PARAGRAF, tittel = "Beskrivelse", tekst = this.beskrivelse)
            else null,
        // Møte med NAV og Samtalereferat
        DetaljUtkast(stil = HALV_LINJE, tittel = "Møteform", tekst = this.moteData?.kanal?.tekst),
        DetaljUtkast(stil = HALV_LINJE, tittel = "Er publisert", tekst = this.moteData?.isReferatPublisert ?.let { if (it) "Ja" else "Nei" } ),
        DetaljUtkast(stil = HEL_LINJE, tittel = "Møtested eller annen praktisk informasjon", tekst = this.moteData?.adresse),
        if (erMoteEllerSamtaleReferat())
            DetaljUtkast(stil = HEL_LINJE, tittel = "Hensikt med møtet", tekst = this.beskrivelse)
            else null,
        DetaljUtkast(stil = HEL_LINJE, tittel = "Forberedelser til møtet", tekst = this.moteData?.forberedelser),
        // Jobbrettet egenaktivitet
        DetaljUtkast(stil = HEL_LINJE, tittel = "Mål med aktiviteten", tekst = this.egenAktivitetData?.hensikt),
        DetaljUtkast(stil = LENKE, tittel = "Lenke", tekst = this.lenke),
        // Jobbsøking
        DetaljUtkast(stil = HEL_LINJE, tittel = "Antall søknader i uka", tekst = this.sokeAvtaleAktivitetData?.antallStillingerIUken.toString()),
        DetaljUtkast(stil = HEL_LINJE, tittel = "Antall søknader i perioden", tekst = this.sokeAvtaleAktivitetData?.antallStillingerSokes.toString()),
        // Stilling fra NAV
        DetaljUtkast(stil = HALV_LINJE, tittel = "Arbeidsgiver", tekst = this.stillingFraNavData?.arbeidsgiver),
        DetaljUtkast(stil = HALV_LINJE, tittel = "Arbeidssted", tekst = this.stillingFraNavData?.arbeidssted),
        DetaljUtkast(stil = HALV_LINJE, tittel = "Svarfrist", tekst = stillingFraNavData?.svarfrist?.let { dateToZonedDateTime(it).format(datoFormat) }),
        DetaljUtkast(stil = HALV_LINJE, tittel = "Søknadsfrist", tekst = this.stillingFraNavData?.soknadsfrist),
        DetaljUtkast(stil = LENKE, tittel = "Les mer om stillingen", tekst = this.stillingFraNavData?.getStillingLenke()),
        // Stilling - opprettes selv
        if (erStilling())
            DetaljUtkast(stil = HALV_LINJE, tittel = "Frist", tekst = tilDato?.let { dateToZonedDateTime(it).format(datoFormat) })
            else null,
        DetaljUtkast(stil = HALV_LINJE, tittel = "Arbeidsgiver", tekst = stillingsSoekAktivitetData?.arbeidsgiver),
        DetaljUtkast(stil = HALV_LINJE, tittel = "Arbeidssted", tekst = stillingsSoekAktivitetData?.arbeidssted),
        DetaljUtkast(stil = HALV_LINJE, tittel = "Kontaktperson", tekst = stillingsSoekAktivitetData?.kontaktPerson),
//        DetaljUtkast(stil = HALV_LINJE, tittel = "Stillingstittel", tekst = tittel), // TODO: Dette overskriver felles-feltet tittel
    )
        .filter { !it.tekst.isNullOrBlank() }
        .map { Detalj(it.stil, it.tittel, it.tekst!!) }
}

private val datoFormat = DateTimeFormatter.ofPattern("dd MMMM uuuu")

fun StillingFraNavData.getStillingLenke(): String {
    return when (isProduction().orElse(false)) {
        true -> "https://www.nav.no/arbeid/stilling/${this.stillingsId}"
        false -> "https://vis-stilling.intern.dev.nav.no/arbeid/stilling/${this.stillingsId}"
    }
}