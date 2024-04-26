package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO
import no.nav.veilarbaktivitet.arena.model.ArenaStatusDTO
import no.nav.veilarbaktivitet.arkivering.*
import no.nav.veilarbaktivitet.arkivering.etiketter.ArkivEtikett
import no.nav.veilarbaktivitet.arkivering.etiketter.ArkivEtikettStil
import no.nav.veilarbaktivitet.util.DateUtils.norskDato

fun ArenaAktivitetDTO.toArkivPayload(meldinger: List<Melding>): ArkivAktivitet =
    ArkivAktivitet(
        tittel = this.tittel,
        type = this.toArkivTypeTekst(),
        status = this.status.toArkivTekst(),
        detaljer = this.toDetaljer(),
        meldinger = meldinger,
        etiketter = this.toArkivEtikett(),
        eksterneHandlinger = emptyList(),
        historikk = AktivitetHistorikk(emptyList())
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
    return listOf(
        Detalj(Stil.HALV_LINJE, "Deltakelsesprosent", tekst = "${this.deltakelseProsent}%"),
        Detalj(Stil.HALV_LINJE, "Arrangør", tekst = this.arrangoer),
        Detalj(Stil.HALV_LINJE, "Dager per uke", tekst = "${this.antallDagerPerUke} dager"),

        )
}

fun ArenaAktivitetDTO.toGruppeaktivitetDetaljer(): List<Detalj> {
    val møteplanTekst = this.moeteplanListe.map {
        if(it.sluttDato.equals(it.startDato) ) {
            " - ${norskDato(it.startDato)} - ${norskDato(it.sluttDato)} ${it.sted}"
    } else {
            " - ${norskDato(it.startDato)} ${it.sted}"
        }
    }.joinToString(separator = "\n")
    return listOf(
        Detalj(Stil.PARAGRAF, "Tidspunkt og sted", tekst = møteplanTekst)
    )
}

fun ArenaAktivitetDTO.toArkivEtikett(): List<ArkivEtikett> {
    val avtaltEtikett = if(this.isAvtalt) ArkivEtikett(ArkivEtikettStil.AVTALT, "Avtalt med NAV") else null
    val statusEtikett = when (this.etikett) {
        ArenaStatusDTO.AKTUELL -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Søkt inn på tiltaket")
        ArenaStatusDTO.AVSLAG -> ArkivEtikett(ArkivEtikettStil.NEGATIVE, "Fått avslag")
        ArenaStatusDTO.IKKAKTUELL -> ArkivEtikett(ArkivEtikettStil.NEUTRAL, "Ikke aktuell for tiltaket")
        ArenaStatusDTO.IKKEM -> ArkivEtikett(ArkivEtikettStil.NEGATIVE, "Ikke møtt på tiltaket")
        ArenaStatusDTO.INFOMOETE -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Infomøte før tiltaket")
        ArenaStatusDTO.JATAKK -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Takket ja til tilbud")
        ArenaStatusDTO.NEITAKK -> ArkivEtikett(ArkivEtikettStil.NEUTRAL, "Takket nei til tilbud")
        ArenaStatusDTO.TILBUD -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "Fått plass på tiltaket")
        ArenaStatusDTO.VENTELISTE -> ArkivEtikett(ArkivEtikettStil.POSITIVE, "På venteliste")
        else -> null
    }
    return listOfNotNull(avtaltEtikett, statusEtikett)
}