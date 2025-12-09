package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO
import no.nav.veilarbaktivitet.arena.model.ArenaStatusEtikettDTO
import no.nav.veilarbaktivitet.arkivering.*
import no.nav.veilarbaktivitet.arkivering.etiketter.ArkivEtikett
import no.nav.veilarbaktivitet.arkivering.etiketter.ArkivEtikettStil
import no.nav.veilarbaktivitet.util.DateUtils.norskDato
import no.nav.veilarbaktivitet.util.toStringWithoutNullDecimals

fun ArenaAktivitetDTO.toArkivPayload(dialogtråd: ArkivDialogtråd?): ArkivAktivitet =
    ArkivAktivitet(
        tittel = this.tittel,
        type = this.toArkivTypeTekst(),
        status = this.status.toArkivTekst(),
        detaljer = this.toDetaljer(),
        dialogtråd = dialogtråd,
        etiketter = this.toArkivEtiketter(),
        eksterneHandlinger = emptyList(),
        historikk = AktivitetHistorikk(emptyList()),
        forhaandsorientering = this.forhaandsorientering?.toArkivForhaandsorientering()
    )

fun ArenaAktivitetDTO.toArkivTypeTekst() =
    when (type) {
        ArenaAktivitetTypeDTO.TILTAKSAKTIVITET -> "Tiltak gjennom NAV"
        ArenaAktivitetTypeDTO.GRUPPEAKTIVITET -> "Gruppeaktivitet"
        ArenaAktivitetTypeDTO.UTDANNINGSAKTIVITET -> "Utdanning"
    }

fun ArenaAktivitetDTO.toDetaljer(): List<Detalj> {
    val fellesDetaljer = listOf(
        Detalj(Stil.PARAGRAF, "Fullført / Tiltak gjennom NAV", tekst = this.beskrivelse),
        Detalj(Stil.HALV_LINJE, "Fra dato", tekst = norskDato(this.fraDato) ),
        Detalj(Stil.HALV_LINJE, "Til dato", tekst = norskDato(this.tilDato) ))

    return when (type) {
        ArenaAktivitetTypeDTO.TILTAKSAKTIVITET -> fellesDetaljer + toTiltaksaktivitetDetaljer()
        ArenaAktivitetTypeDTO.GRUPPEAKTIVITET -> fellesDetaljer + toGruppeaktivitetDetaljer()
        ArenaAktivitetTypeDTO.UTDANNINGSAKTIVITET -> fellesDetaljer
    }
}

fun ArenaAktivitetDTO.toTiltaksaktivitetDetaljer(): List<Detalj> {
    return listOfNotNull(
        Detalj(Stil.HALV_LINJE, "Deltakelsesprosent", tekst = "${this.deltakelseProsent.toStringWithoutNullDecimals()}%"),
        Detalj(Stil.HALV_LINJE, "Arrangør", tekst = this.arrangoer),
        if(this.antallDagerPerUke != null) Detalj(Stil.HALV_LINJE, "Dager per uke", tekst = "${this.antallDagerPerUke.toStringWithoutNullDecimals()}") else null,
        )
}

fun ArenaAktivitetDTO.toGruppeaktivitetDetaljer(): List<Detalj> {
    val møteplanTekst = this.moeteplanListe.map {
        if(!it.startDato.equals(it.sluttDato) ) {
            " - ${norskDato(it.startDato)} - ${norskDato(it.sluttDato)} ${it.sted}"
    } else {
            " - ${norskDato(it.startDato)} ${it.sted}"
        }
    }.joinToString(separator = "\n")
    return listOf(
        Detalj(Stil.PARAGRAF, "Tidspunkt og sted", tekst = møteplanTekst)
    )
}

fun ArenaAktivitetDTO.toArkivEtiketter(): List<ArkivEtikett> {
    val avtaltEtikett = if(this.isAvtalt) ArkivEtikett(ArkivEtikettStil.AVTALT, "Avtalt med NAV") else null
    val statusEtikett = this.etikett?.toArkivEtikett()
    return listOfNotNull(avtaltEtikett, statusEtikett)
}

fun ArenaStatusEtikettDTO.toArkivEtikett(): ArkivEtikett {
    return when (this) {
        ArenaStatusEtikettDTO.AKTUELL -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Søkt inn på tiltaket")
        ArenaStatusEtikettDTO.AVSLAG -> ArkivEtikett(ArkivEtikettStil.NEGATIVE, "Fått avslag")
        ArenaStatusEtikettDTO.IKKAKTUELL -> ArkivEtikett(ArkivEtikettStil.NEUTRAL, "Ikke aktuell for tiltaket")
        ArenaStatusEtikettDTO.IKKEM -> ArkivEtikett(ArkivEtikettStil.NEGATIVE, "Ikke møtt på tiltaket")
        ArenaStatusEtikettDTO.INFOMOETE -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Infomøte før tiltaket")
        ArenaStatusEtikettDTO.JATAKK -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Takket ja til tilbud")
        ArenaStatusEtikettDTO.NEITAKK -> ArkivEtikett(ArkivEtikettStil.NEUTRAL, "Takket nei til tilbud")
        ArenaStatusEtikettDTO.TILBUD -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Fått plass på tiltaket")
        ArenaStatusEtikettDTO.VENTELISTE -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "På venteliste")
    }
}