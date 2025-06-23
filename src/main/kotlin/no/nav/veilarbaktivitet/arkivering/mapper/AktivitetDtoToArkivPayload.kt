package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.common.utils.EnvironmentUtils.isProduction
import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData.*
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.LenkeType
import no.nav.veilarbaktivitet.arkivering.ArkivAktivitet
import no.nav.veilarbaktivitet.arkivering.ArkivDialogtråd
import no.nav.veilarbaktivitet.arkivering.Detalj
import no.nav.veilarbaktivitet.arkivering.EksternHandling
import no.nav.veilarbaktivitet.arkivering.Stil.*
import no.nav.veilarbaktivitet.arkivering.etiketter.getArkivEtiketter
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.stilling_fra_nav.CvKanDelesData
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData
import no.nav.veilarbaktivitet.util.DateUtils.*
import java.time.Duration

fun AktivitetData.toArkivPayload(dialogtråd: ArkivDialogtråd?, historikk: Historikk): ArkivAktivitet {
    return ArkivAktivitet(
        tittel = this.tittel,
        type = this.toArkivTypeTekst(),
        status = this.status.toArkivTekst(),
        detaljer = this.toDetaljer(),
        dialogtråd = dialogtråd,
        etiketter = this.getArkivEtiketter(),
        eksterneHandlinger = this.getEksterneHandlinger(),
        historikk = historikk.tilAktivitetHistorikk(),
        forhaandsorientering = this.forhaandsorientering?.toArkivForhaandsorientering()
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
        MOTE -> "Møte med Nav"
        SAMTALEREFERAT -> "Samtalereferat"
        STILLING_FRA_NAV -> "Stilling fra Nav"
        EKSTERNAKTIVITET -> {
            when (this.eksternAktivitetData.type!!) {
                AktivitetskortType.ARENA_TILTAK -> "Tiltak gjennom Nav"
                AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD -> "Avtale midlertidig lønnstilskudd"
                AktivitetskortType.VARIG_LONNSTILSKUDD -> "Avtale varig lønnstilskudd"
                AktivitetskortType.MENTOR -> "Mentor"
                AktivitetskortType.ARBEIDSTRENING -> "Arbeidstrening"
                AktivitetskortType.VARIG_TILRETTELAGT_ARBEID_I_ORDINAER_VIRKSOMHET -> "Varig tilrettelagt arbeid i ordinær virksomhet"
                AktivitetskortType.INDOPPFAG -> "Oppfølging"
                AktivitetskortType.ARBFORB -> "Arbeidsforberedende trening"
                AktivitetskortType.AVKLARAG -> "Avklaring"
                AktivitetskortType.VASV -> "Varig tilrettelagt arbeid i skjermet virksomhet"
                AktivitetskortType.ARBRRHDAG -> "Arbeidsrettet rehabilitering"
                AktivitetskortType.DIGIOPPARB -> "Digitalt jobbsøkerkurs"
                AktivitetskortType.JOBBK -> "Jobbklubb"
                AktivitetskortType.GRUPPEAMO -> "Arbeidsmarkedsopplæring (Gruppe)"
                AktivitetskortType.GRUFAGYRKE -> "Fag- og yrkesopplæring (Gruppe)"
                AktivitetskortType.REKRUTTERINGSTREFF -> "Rekrutteringstreff"
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
    ?.map { Detalj(HEL_LINJE, it.label, it.verdi) } ?: emptyList()


fun AktivitetData.getEksterneHandlinger(): List<EksternHandling> =
    this.eksternAktivitetData?.handlinger?.filter { it.lenkeType == LenkeType.INTERN || it.lenkeType == LenkeType.FELLES }
        ?.map {
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
    Detalj(stil = HALV_LINJE, tittel = "Dato", tekst = norskDato(fraDato)),
    Detalj(stil = HALV_LINJE, tittel = "Klokkeslett", tekst = klokkeslett(fraDato)),
    Detalj(stil = HALV_LINJE, tittel = "Møteform", tekst = moteData?.kanal?.tekst),
    Detalj(stil = HALV_LINJE, tittel = "Varighet", tekst = beregnVarighet()),
    Detalj(stil = HEL_LINJE, tittel = "Møtested eller annen praktisk informasjon", tekst = moteData?.adresse),
    Detalj(stil = HEL_LINJE, tittel = "Hensikt med møtet", tekst = beskrivelse),
    Detalj(stil = HEL_LINJE, tittel = "Forberedelser til møtet", tekst = moteData?.forberedelser),
    Detalj(
        stil = PARAGRAF,
        tittel = "Samtalereferat",
        tekst = if (moteData?.isReferatPublisert == true) moteData?.referat else ""
    ),
)

fun AktivitetData.toEgenaktivitetDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Fra dato", tekst = norskDato(fraDato)),
    Detalj(stil = HALV_LINJE, tittel = "Til dato", tekst = norskDato(tilDato)),
    Detalj(stil = HEL_LINJE, tittel = "Mål med aktiviteten", tekst = egenAktivitetData?.hensikt),
    Detalj(stil = HEL_LINJE, tittel = "Min huskeliste", tekst = egenAktivitetData?.oppfolging),
    Detalj(stil = PARAGRAF, tittel = "Beskrivelse", tekst = beskrivelse),
    Detalj(stil = LENKE, tittel = "Lenke", tekst = lenke),
)

fun AktivitetData.toStillingDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Fra dato", tekst = norskDato(fraDato)),
    Detalj(stil = HALV_LINJE, tittel = "Frist", tekst = norskDato(tilDato)),
    Detalj(stil = HALV_LINJE, tittel = "Arbeidsgiver", tekst = stillingsSoekAktivitetData?.arbeidsgiver),
    Detalj(stil = HALV_LINJE, tittel = "Kontaktperson", tekst = stillingsSoekAktivitetData?.kontaktPerson),
    Detalj(stil = HALV_LINJE, tittel = "Arbeidssted", tekst = stillingsSoekAktivitetData?.arbeidssted),
    Detalj(stil = PARAGRAF, tittel = "Beskrivelse", tekst = beskrivelse),
    Detalj(stil = LENKE, tittel = "Lenke til stillingsannonsen", tekst = lenke),
)

fun AktivitetData.toStillingFraNavDetaljer(): List<Detalj> {
    val detaljer = listOf(
        Detalj(stil = HALV_LINJE, tittel = "Arbeidsgiver", tekst = stillingFraNavData?.arbeidsgiver),
        Detalj(stil = HALV_LINJE, tittel = "Arbeidssted", tekst = stillingFraNavData?.arbeidssted),
        Detalj(stil = HALV_LINJE, tittel = "Svarfrist", tekst = norskDato(stillingFraNavData?.svarfrist)),
        Detalj(stil = HALV_LINJE, tittel = "Søknadsfrist", tekst = stillingFraNavData?.soknadsfrist),
        Detalj(stil = LENKE, tittel = "Les mer om stillingen", tekst = stillingFraNavData?.getStillingLenke())
    )
    val harSvart = stillingFraNavData.cvKanDelesData != null
    if (harSvart) {
        val cvKanDelesData = stillingFraNavData.cvKanDelesData
        val tittel = if (stillingFraNavData.cvKanDelesData.kanDeles) "Du svarte at du er interessert" else "Du svarte at du ikke er interessert"
        val svarOgEndretTekst = if(stillingFraNavData.cvKanDelesData.endretAvType == Innsender.BRUKER) cvKanDelesData.svarOgEndretTekstBruker() else cvKanDelesData.svarOgEndretTekstVeileder()
        val oppfolgingsTekst = if(cvKanDelesData.kanDeles) "Arbeidsgiveren eller Nav vil kontakte deg hvis du er aktuell for stillingen." else null
        val fullTekst = oppfolgingsTekst?.let { "$svarOgEndretTekst\n\n$it" } ?: svarOgEndretTekst

        val svarDetalj = Detalj(stil = PARAGRAF, tittel = tittel, tekst = fullTekst)
        return detaljer + svarDetalj
    } else {
        return detaljer
    }
}

private fun CvKanDelesData.svarOgEndretTekstBruker(): String {
    val svarTekst = if (this.kanDeles) {
        "Ja, og Nav kan dele CV-en min med denne arbeidsgiveren."
    } else {
        "Nei, og jeg vil ikke at Nav skal dele CV-en min med arbeidsgiveren."
    }
    return "$svarTekst\n\nDu svarte ${norskDato(this.endretTidspunkt)}."
}

private fun CvKanDelesData.svarOgEndretTekstVeileder(): String {
    val svarTekst = "Nav var i kontakt med deg ${norskDato(this.avtaltDato)}. Du sa ${if (this.kanDeles) "Ja" else "Nei"} til at CV-en din deles med arbeidsgiver."
    return "$svarTekst\n\nNav svarte på vegne av deg ${norskDato(this.endretTidspunkt)}."
}

fun AktivitetData.toSokeAvtaleDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Fra dato", tekst = norskDato(fraDato)),
    Detalj(stil = HALV_LINJE, tittel = "Til dato", tekst = norskDato(tilDato)),
    Detalj(
        stil = HEL_LINJE,
        tittel = "Antall søknader i perioden",
        tekst = sokeAvtaleAktivitetData?.antallStillingerSokes.toString()
    ),
    Detalj(
        stil = HEL_LINJE,
        tittel = "Antall søknader i uka",
        tekst = sokeAvtaleAktivitetData?.antallStillingerIUken.toString()
    ),
    Detalj(stil = PARAGRAF, tittel = "Beskrivelse", tekst = beskrivelse),
)

fun AktivitetData.toBehandlingDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Type behandling", tekst = behandlingAktivitetData?.behandlingType),
    Detalj(stil = HALV_LINJE, tittel = "Behandlingssted", tekst = behandlingAktivitetData?.behandlingSted),
    Detalj(stil = HALV_LINJE, tittel = "Fra dato", tekst = norskDato(fraDato)),
    Detalj(stil = HALV_LINJE, tittel = "Til dato", tekst = norskDato(tilDato)),
    Detalj(stil = HEL_LINJE, tittel = "Mål for behandlingen", tekst = behandlingAktivitetData?.effekt),
    Detalj(stil = HEL_LINJE, tittel = "Oppfølging fra Nav", tekst = behandlingAktivitetData?.behandlingOppfolging),
    Detalj(stil = PARAGRAF, tittel = "Beskrivelse", tekst = beskrivelse),
)

fun AktivitetData.toIJobbDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Fra dato", tekst = norskDato(fraDato)),
    Detalj(stil = HALV_LINJE, tittel = "Til dato", tekst = norskDato(tilDato)),
    Detalj(stil = HALV_LINJE, tittel = "Stillingsandel", tekst = iJobbAktivitetData.jobbStatusType.toString()),
    Detalj(stil = HALV_LINJE, tittel = "Arbeidsgiver", tekst = iJobbAktivitetData.ansettelsesforhold),
    Detalj(stil = HALV_LINJE, tittel = "Ansettelsesforhold", tekst = iJobbAktivitetData.arbeidstid),
    Detalj(stil = PARAGRAF, tittel = "Beskrivelse", tekst = beskrivelse),
)

fun AktivitetData.toEksternAktivitetDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Fra dato", tekst = norskDato(fraDato)),
    Detalj(stil = HALV_LINJE, tittel = "Til dato", tekst = norskDato(tilDato)),
) + hentEksterneDetaljer() + listOf(Detalj(stil = PARAGRAF, tittel = "Beskrivelse", tekst = beskrivelse))

fun AktivitetData.toSamtalereferatDetaljer() = listOf(
    Detalj(stil = HALV_LINJE, tittel = "Dato", tekst = norskDato(fraDato)),
    Detalj(stil = HALV_LINJE, tittel = "Møteform", tekst = moteData?.kanal?.tekst),
    Detalj(stil = PARAGRAF, tittel = "Samtalereferat", tekst = moteData?.referat),
)

fun StillingFraNavData.getStillingLenke(): String {
    return when (isProduction().orElse(false)) {
        true -> "https://www.nav.no/arbeid/stilling/${this.stillingsId}"
        false -> "https://vis-stilling.intern.dev.nav.no/arbeid/stilling/${this.stillingsId}"
    }
}
