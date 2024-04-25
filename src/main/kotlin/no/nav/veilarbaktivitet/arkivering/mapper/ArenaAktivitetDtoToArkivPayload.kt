package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO
import no.nav.veilarbaktivitet.arkivering.*
import no.nav.veilarbaktivitet.arkivering.etiketter.ArkivEtikett
import no.nav.veilarbaktivitet.arkivering.etiketter.ArkivEtikettStil
import no.nav.veilarbaktivitet.util.DateUtils.norskDato

fun ArenaAktivitetDTO.toArkivPayload(meldinger: List<Melding>): ArkivAktivitet =
    ArkivAktivitet(
        tittel = this.tittel,
        type = this.toArkivTypeTekst(),
        status = this.status.toArkivTekst(),
        detaljer = emptyList(), // TODO
        meldinger = meldinger,
        etiketter = listOf(ArkivEtikett(ArkivEtikettStil.AVTALT, "Avtalt med NAV")), // TODO: Usikker på om dette er riktig
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
//    val forhaandsorienteringDetalj = Detalj(Stil.PARAGRAF, "Forhåndsorientering", tekst = this.forhaandsorientering) TODO

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

// Skal vi se på avtalt feltet?
// Kan dette være riktig?
//
//fun ArenaAktivitetDTO.toArkivEtikett() =
//    when (this.etikett) {
//        ArenaStatusDTO.AKTUELL -> TODO()
//        ArenaStatusDTO.AVSLAG -> TODO()
//        ArenaStatusDTO.IKKAKTUELL -> TODO()
//        ArenaStatusDTO.IKKEM -> TODO()
//        ArenaStatusDTO.INFOMOETE -> TODO()
//        ArenaStatusDTO.JATAKK -> TODO()
//        ArenaStatusDTO.NEITAKK -> TODO()
//        ArenaStatusDTO.TILBUD -> TODO()
//        ArenaStatusDTO.VENTELISTE -> TODO()
//    }
