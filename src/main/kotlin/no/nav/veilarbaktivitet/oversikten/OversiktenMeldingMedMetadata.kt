package no.nav.veilarbaktivitet.oversikten

import no.nav.common.types.identer.Fnr
import java.time.ZonedDateTime
import java.util.*

open class OversiktenMeldingMedMetadata(
    val meldingKey: MeldingKey,
    val fnr: Fnr,
    val opprettet: ZonedDateTime = ZonedDateTime.now(),
    val tidspunktSendt: ZonedDateTime? = null,
    val utsendingStatus: UtsendingStatus = UtsendingStatus.SKAL_SENDES,
    val meldingSomJson: String,
    val kategori: OversiktenMelding.Kategori,
    val operasjon: OversiktenMelding.Operasjon,
)

class LagretOversiktenMeldingMedMetadata(
    val id: Long,
    meldingKey: MeldingKey,
    fnr: Fnr,
    opprettet: ZonedDateTime,
    tidspunktSendt: ZonedDateTime?,
    utsendingStatus: UtsendingStatus,
    meldingSomJson: String,
    kategori: OversiktenMelding.Kategori,
    operasjon: OversiktenMelding.Operasjon
) : OversiktenMeldingMedMetadata(
    meldingKey = meldingKey,
    fnr = fnr,
    opprettet = opprettet,
    tidspunktSendt = tidspunktSendt,
    utsendingStatus = utsendingStatus,
    meldingSomJson = meldingSomJson,
    kategori = kategori,
    operasjon = operasjon
)

typealias MeldingKey = UUID

enum class UtsendingStatus {
    SKAL_SENDES,
    SENDT,
    SKAL_IKKE_SENDES
}
