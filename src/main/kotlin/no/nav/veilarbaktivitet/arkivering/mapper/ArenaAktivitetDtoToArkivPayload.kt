package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO
import no.nav.veilarbaktivitet.arkivering.AktivitetHistorikk
import no.nav.veilarbaktivitet.arkivering.ArkivAktivitet
import no.nav.veilarbaktivitet.arkivering.Melding
import no.nav.veilarbaktivitet.arkivering.etiketter.ArkivEtikett
import no.nav.veilarbaktivitet.arkivering.etiketter.ArkivEtikettStil

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
