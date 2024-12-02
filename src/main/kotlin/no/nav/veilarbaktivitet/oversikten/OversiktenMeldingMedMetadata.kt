package no.nav.veilarbaktivitet.oversikten

import no.nav.common.types.identer.Fnr
import java.time.ZonedDateTime
import java.util.*

data class OversiktenMeldingMedMetadata(
    val meldingKey: MeldingKey,
    val fnr: Fnr,
    val opprettet: ZonedDateTime = ZonedDateTime.now(),
    val tidspunktSendt: ZonedDateTime? = null,
    val utsendingStatus: UtsendingStatus = UtsendingStatus.SKAL_SENDES,
    val meldingSomJson: String,
    val kategori: OversiktenMelding.Kategori,
) {
    fun tilSendtMeldingMedMetadata() = this.copy(tidspunktSendt = ZonedDateTime.now(), utsendingStatus = UtsendingStatus.SENDT)
}

typealias MeldingKey = UUID

enum class UtsendingStatus {
    SKAL_SENDES,
    SENDT,
    SKAL_IKKE_SENDES
}
