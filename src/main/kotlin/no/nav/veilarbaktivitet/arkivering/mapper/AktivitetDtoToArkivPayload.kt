package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.common.utils.EnvironmentUtils.isProduction
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData.*
import no.nav.veilarbaktivitet.aktivitet.domain.EksternAktivitetData
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.arkivering.ArkivAktivitet
import no.nav.veilarbaktivitet.arkivering.Detalj
import no.nav.veilarbaktivitet.arkivering.EksternHandling
import no.nav.veilarbaktivitet.arkivering.Melding
import no.nav.veilarbaktivitet.arkivering.Stil.*
import no.nav.veilarbaktivitet.arkivering.etiketter.getArkivEtiketter
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData
import no.nav.veilarbaktivitet.util.DateUtils.dateToZonedDateTime
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

fun AktivitetData.toArkivPayload(meldinger: List<Melding>): ArkivAktivitet {
    return ArkivAktivitet(
        tittel = this.tittel,
        type = this.toArkivTypeTekst(),
        status = this.status.toArkivTekst(),
        detaljer = this.toDetaljer(),
        meldinger = meldinger,
        etiketter = this.getArkivEtiketter(),
        eksterneHandlinger = this.getEksterneHandlinger(),
    )
}

fun AktivitetStatus.toArkivTekst(): String {
    return when (this) {
        AktivitetStatus.AVBRUTT -> "Avbrutt"
        AktivitetStatus.BRUKER_ER_INTERESSERT -> "Forslag"
        AktivitetStatus.FULLFORT -> "Fullført"
        AktivitetStatus.GJENNOMFORES -> "Gjennomføres"
        AktivitetStatus.PLANLAGT -> "Planlagt"
    }
}

fun AktivitetData.toArkivTypeTekst(): String {
    return when (this.aktivitetType) {
        EGENAKTIVITET -> "Jobbrettet egenaktivitet"
        JOBBSOEKING -> "Stilling"
        SOKEAVTALE -> "Jobbsøking"
        IJOBB -> "Jobb jeg har nå"
        BEHANDLING -> "Behandling"
        MOTE -> "Møte med NAV"
        SAMTALEREFERAT -> "Samtalereferat"
        STILLING_FRA_NAV -> "Stilling fra NAV"
        EKSTERNAKTIVITET -> {
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
    }
}

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

fun AktivitetData.hentEksterneDetaljer(): List<Detalj> = this.eksternAktivitetData
    ?.detaljer
    ?.map { Detalj(HALV_LINJE, it.label, it.verdi) } ?: emptyList()


fun AktivitetData.getEksterneHandlinger(): List<EksternHandling> =
    this.eksternAktivitetData?.handlinger?.map {
        EksternHandling(
            tekst = it.tekst,
            subtekst = it.subtekst,
            url = it.url.toString(),
        )
    } ?: emptyList()

fun AktivitetData.toDetaljer(): List<Detalj> =
    when (aktivitetType) {
        MOTE -> toMoteDetaljer()
        EGENAKTIVITET -> toEgenaktivitetDetaljer()
        JOBBSOEKING -> toStillingDetaljer()
        SOKEAVTALE -> toSokeAvtaleDetaljer()
        IJOBB -> toIJobbDetaljer()
        BEHANDLING -> toBehandlingDetaljer()
        SAMTALEREFERAT -> toSamtalereferatDetaljer()
        STILLING_FRA_NAV -> toStillingFraNavDetaljer()
        EKSTERNAKTIVITET -> toEksternAktivitetDetaljer()
    }

fun AktivitetData.toMoteDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Dato", tekst = fraDato.norskDato()),
    Detalj(stil = HALV_LINJE, tittel = "Klokkeslett", tekst = fraDato.klokkeslett()),
    Detalj(stil = HALV_LINJE, tittel = "Møteform", tekst = moteData?.kanal?.tekst),
    Detalj(stil= HALV_LINJE, tittel = "Varighet", tekst = beregnVarighet()),
    Detalj(stil = HEL_LINJE, tittel = "Møtested eller annen praktisk informasjon", tekst = moteData?.adresse),
    Detalj(stil = HEL_LINJE, tittel = "Hensikt med møtet", tekst = beskrivelse),
    Detalj(stil = HEL_LINJE, tittel = "Forberedelser til møtet", tekst = moteData?.forberedelser),
    Detalj(stil = PARAGRAF, tittel = "Samtalereferat", tekst = moteData?.referat),
)

fun AktivitetData.toEgenaktivitetDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Fra dato", tekst = fraDato.norskDato()),
    Detalj(stil = HALV_LINJE, tittel = "Til dato", tekst = tilDato.norskDato()),
    Detalj(stil = HEL_LINJE, tittel = "Mål med aktiviteten", tekst = egenAktivitetData?.hensikt),
    Detalj(stil = HEL_LINJE, tittel = "Min huskeliste", tekst = egenAktivitetData?.oppfolging),
    Detalj(stil = PARAGRAF, tittel = "Beskrivelse", tekst = beskrivelse),
    Detalj(stil = LENKE, tittel = "Lenke", tekst = lenke),
)

fun AktivitetData.toStillingDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Fra dato", tekst = fraDato.norskDato()),
    Detalj(stil = HALV_LINJE, tittel = "Frist", tekst = tilDato.norskDato()),
    Detalj(stil = HALV_LINJE, tittel = "Arbeidsgiver", tekst = stillingsSoekAktivitetData?.arbeidsgiver),
    Detalj(stil = HALV_LINJE, tittel = "Kontaktperson", tekst = stillingsSoekAktivitetData?.kontaktPerson),
    Detalj(stil = HALV_LINJE, tittel = "Arbeidssted", tekst = stillingsSoekAktivitetData?.arbeidssted),
    Detalj(stil = PARAGRAF, tittel = "Beskrivelse", tekst = beskrivelse),
    Detalj(stil = LENKE, tittel = "Lenke til stillingsannonsen", tekst = lenke),
)

fun AktivitetData.toStillingFraNavDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Arbeidsgiver", tekst = stillingFraNavData?.arbeidsgiver),
    Detalj(stil = HALV_LINJE, tittel = "Arbeidssted", tekst = stillingFraNavData?.arbeidssted),
    Detalj(stil = HALV_LINJE, tittel = "Svarfrist", tekst = stillingFraNavData?.svarfrist?.norskDato()),
    Detalj(stil = HALV_LINJE, tittel = "Søknadsfrist", tekst = stillingFraNavData?.soknadsfrist),
    Detalj(stil = LENKE, tittel = "Les mer om stillingen", tekst = stillingFraNavData?.getStillingLenke()),
)

fun AktivitetData.toSokeAvtaleDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Fra dato", tekst = fraDato.norskDato()),
    Detalj(stil = HALV_LINJE, tittel = "Til dato", tekst = tilDato.norskDato()),
    Detalj(stil = HEL_LINJE, tittel = "Antall søknader i perioden", tekst = sokeAvtaleAktivitetData?.antallStillingerSokes.toString()),
    Detalj(stil = HEL_LINJE, tittel = "Antall søknader i uka", tekst = sokeAvtaleAktivitetData?.antallStillingerIUken.toString()),
    Detalj(stil = PARAGRAF, tittel = "Beskrivelse", tekst = beskrivelse),
)

fun AktivitetData.toBehandlingDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Type behandling", tekst = behandlingAktivitetData?.behandlingType),
    Detalj(stil = HALV_LINJE, tittel = "Behandlingssted", tekst = behandlingAktivitetData?.behandlingSted),
    Detalj(stil = HALV_LINJE, tittel = "Fra dato", tekst = fraDato.norskDato()),
    Detalj(stil = HALV_LINJE, tittel = "Til dato", tekst = tilDato.norskDato()),
    Detalj(stil = HEL_LINJE, tittel = "Mål for behandlingen", tekst = behandlingAktivitetData?.effekt),
    Detalj(stil = HEL_LINJE, tittel = "Oppfølging fra NAV", tekst = behandlingAktivitetData?.behandlingOppfolging),
    Detalj(stil = PARAGRAF, tittel = "Beskrivelse", tekst = beskrivelse),
)

fun AktivitetData.toIJobbDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Fra dato", tekst = fraDato.norskDato()),
    Detalj(stil = HALV_LINJE, tittel = "Til dato", tekst = tilDato.norskDato()),
    Detalj(stil = HALV_LINJE, tittel = "Stillingsandel", tekst = iJobbAktivitetData.jobbStatusType.toString()),
    Detalj(stil = HALV_LINJE, tittel = "Arbeidsgiver", tekst = iJobbAktivitetData.ansettelsesforhold),
    Detalj(stil = HALV_LINJE, tittel = "Ansettelsesforhold", tekst = iJobbAktivitetData.arbeidstid),
    Detalj(stil = PARAGRAF, tittel = "Beskrivelse", tekst = beskrivelse),
)

fun AktivitetData.toEksternAktivitetDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Fra dato", tekst = fraDato.norskDato()),
    Detalj(stil = HALV_LINJE, tittel = "Til dato", tekst = tilDato.norskDato()),
) + hentEksterneDetaljer() + listOf(Detalj(stil = PARAGRAF, tittel = "Beskrivelse", tekst = beskrivelse))

fun AktivitetData.toSamtalereferatDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Dato", tekst = fraDato.norskDato()),
    Detalj(stil = HALV_LINJE, tittel = "Møteform", tekst = moteData?.kanal?.tekst),
    Detalj(stil = PARAGRAF, tittel = "Samtalereferat", tekst = moteData?.referat),
)

fun StillingFraNavData.getStillingLenke(): String {
    return when (isProduction().orElse(false)) {
        true -> "https://www.nav.no/arbeid/stilling/${this.stillingsId}"
        false -> "https://vis-stilling.intern.dev.nav.no/arbeid/stilling/${this.stillingsId}"
    }
}

fun Date?.klokkeslett(): String {
    return this?.let {
        dateToZonedDateTime(it).format(norskKlokkeslettformat)
    } ?: ""
}

fun ZonedDateTime.klokkeslett(): String =
    withZoneSameInstant(ZoneId.of("Europe/Oslo"))
        .format(norskKlokkeslettformat)

fun Date?.norskDato() = this?.let {
    dateToZonedDateTime(it).format(norskDatoformat)
} ?: ""

fun ZonedDateTime.norskDato(): String =
    withZoneSameInstant(ZoneId.of("Europe/Oslo"))
        .format(norskDatoformat)

private val norskDatoformat = DateTimeFormatter.ofPattern("dd MMMM uuuu", Locale.forLanguageTag("no"))
private val norskKlokkeslettformat = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("no"))