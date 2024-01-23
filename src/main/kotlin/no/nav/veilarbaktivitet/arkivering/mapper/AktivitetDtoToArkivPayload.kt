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
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.*

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
fun AktivitetData.erMote() = this.aktivitetType in listOf(AktivitetTypeData.MOTE)
fun AktivitetData.erStilling() = this.aktivitetType in listOf(AktivitetTypeData.JOBBSOEKING)

fun AktivitetData.erSokeavtale() = this.aktivitetType in listOf(AktivitetTypeData.SOKEAVTALE)

fun AktivitetData.beregnVarighet(): String {
    val varighet = Duration.between(dateToZonedDateTime(fraDato), dateToZonedDateTime(tilDato))
    val timer = varighet.toHours()
    val minutter = varighet.toMinutesPart()
    val timeString = when {
        timer == 0L -> null
        timer == 1L -> "$timer time"
        else -> "$timer timer"
    }
    val minutterString = when {
        minutter == 0 -> null
        minutter == 1 -> "$minutter minutt"
        else -> "$minutter minutter"
    }
    return listOfNotNull(timeString, minutterString).joinToString(", ")
}

fun AktivitetData.klokkeslett(): String {
    return dateToZonedDateTime(fraDato).format(klokkeslettFormat)
}

fun AktivitetData.toDetaljer(): List<Detalj> {
    return listOfNotNull(
        if (!erMoteEllerSamtaleReferat())
            DetaljUtkast(stil = HALV_LINJE, tittel = "Fra dato", tekst = fraDato.let { dateToZonedDateTime(it).format(datoFormat) })
            else null,
        if (!erStilling() && !erMoteEllerSamtaleReferat())
            DetaljUtkast(stil = HALV_LINJE, tittel = "Til dato", tekst = tilDato?.let { dateToZonedDateTime(it).format(datoFormat) }) // TODO: Trenger klokkeslett og varighet på møte med nav, skal hete til-data
            else null,
        if(!erMoteEllerSamtaleReferat())
            DetaljUtkast(stil = PARAGRAF, tittel = "Beskrivelse", tekst = beskrivelse)
            else null,
        // Møte med NAV og Samtalereferat
        if (erMoteEllerSamtaleReferat())
            DetaljUtkast(stil = HALV_LINJE, tittel = "Dato", tekst = fraDato.let { dateToZonedDateTime(it).format(datoFormat) })
        else null,
        if (erMote()) DetaljUtkast(stil = HALV_LINJE, tittel = "Klokkeslett", tekst = klokkeslett()) else null,
        DetaljUtkast(stil = HALV_LINJE, tittel = "Møteform", tekst = moteData?.kanal?.tekst),
        if(erMote()) DetaljUtkast(stil= HALV_LINJE, tittel = "Varighet", tekst = beregnVarighet()) else null,
        if (erMote()) DetaljUtkast(stil = HEL_LINJE, tittel = "Møtested eller annen praktisk informasjon", tekst = moteData?.adresse) else null,
        if (erMote())
            DetaljUtkast(stil = HEL_LINJE, tittel = "Hensikt med møtet", tekst = beskrivelse)
            else null,
        if(erMote()) DetaljUtkast(stil = HEL_LINJE, tittel = "Forberedelser til møtet", tekst = moteData?.forberedelser) else null,
        if(erMoteEllerSamtaleReferat()) DetaljUtkast(stil = HEL_LINJE, tittel = "Samtalereferat", tekst = moteData?.referat) else null,
        // Jobbrettet egenaktivitet
        DetaljUtkast(stil = HEL_LINJE, tittel = "Mål med aktiviteten", tekst = egenAktivitetData?.hensikt),
        if(!erMoteEllerSamtaleReferat()) DetaljUtkast(stil = LENKE, tittel = "Lenke", tekst = lenke) else null,
        // Jobbsøking
        if(erSokeavtale()) DetaljUtkast(stil = HEL_LINJE, tittel = "Antall søknader i uka", tekst = sokeAvtaleAktivitetData?.antallStillingerIUken.toString()) else null,
        if(erSokeavtale()) DetaljUtkast(stil = HEL_LINJE, tittel = "Antall søknader i perioden", tekst = sokeAvtaleAktivitetData?.antallStillingerSokes.toString()) else null,
        // Stilling fra NAV
        DetaljUtkast(stil = HALV_LINJE, tittel = "Arbeidsgiver", tekst = stillingFraNavData?.arbeidsgiver),
        DetaljUtkast(stil = HALV_LINJE, tittel = "Arbeidssted", tekst = stillingFraNavData?.arbeidssted),
        DetaljUtkast(stil = HALV_LINJE, tittel = "Svarfrist", tekst = stillingFraNavData?.svarfrist?.let { dateToZonedDateTime(it).format(datoFormat) }),
        DetaljUtkast(stil = HALV_LINJE, tittel = "Søknadsfrist", tekst = stillingFraNavData?.soknadsfrist),
        DetaljUtkast(stil = LENKE, tittel = "Les mer om stillingen", tekst = stillingFraNavData?.getStillingLenke()),
        // Stilling - opprettes selv
        if (erStilling())
            DetaljUtkast(stil = HALV_LINJE, tittel = "Frist", tekst = tilDato?.let { dateToZonedDateTime(it).format(datoFormat) })
            else null,
        DetaljUtkast(stil = HALV_LINJE, tittel = "Arbeidsgiver", tekst = stillingsSoekAktivitetData?.arbeidsgiver),
        DetaljUtkast(stil = HALV_LINJE, tittel = "Arbeidssted", tekst = stillingsSoekAktivitetData?.arbeidssted),
        DetaljUtkast(stil = HALV_LINJE, tittel = "Kontaktperson", tekst = stillingsSoekAktivitetData?.kontaktPerson),
    )
        .filter { !it.tekst.isNullOrBlank() }
        .map { Detalj(it.stil, it.tittel, it.tekst!!) }
}

private val datoFormat = DateTimeFormatter.ofPattern("dd MMMM uuuu", Locale.forLanguageTag("no"))
private val klokkeslettFormat = DateTimeFormatter.ofPattern("hh:mm", Locale.forLanguageTag("no"))

fun StillingFraNavData.getStillingLenke(): String {
    return when (isProduction().orElse(false)) {
        true -> "https://www.nav.no/arbeid/stilling/${this.stillingsId}"
        false -> "https://vis-stilling.intern.dev.nav.no/arbeid/stilling/${this.stillingsId}"
    }
}