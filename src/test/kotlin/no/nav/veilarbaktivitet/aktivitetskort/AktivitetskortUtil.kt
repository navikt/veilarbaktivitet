package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.Ident
import no.nav.veilarbaktivitet.aktivitetskort.dto.Aktivitetskort
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.person.Innsender
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

object AktivitetskortUtil {
    @JvmStatic
    fun ny(
        funksjonellId: UUID,
        aktivitetStatus: AktivitetStatus,
        endretTidspunkt: ZonedDateTime,
        mockBruker: MockBruker
    ): Aktivitetskort {
        return Aktivitetskort(
            funksjonellId,
            mockBruker.fnr,
            "The Elder Scrolls: Arena",
            "arenabeskrivelse",
            aktivitetStatus,
            LocalDate.now().minusDays(30),
            LocalDate.now().minusDays(30),
            Ident("arenaEndretav", Innsender.ARENAIDENT),
            endretTidspunkt,
            true,
            null,
            null,
            listOf(
                Attributt("arrangørnavn", "Arendal"),
                Attributt("deltakelsesprosent", "40%"),
                Attributt("dager per uke", "2")
            ),
            listOf(Etikett("SOKT_INN"))
        )
    }

    @JvmStatic
    fun aktivitetskortMelding(
        payload: Aktivitetskort,
        messageId: UUID = UUID.randomUUID(),
        aktivitetskortType: AktivitetskortType,
        source: MessageSource
    ): KafkaAktivitetskortWrapperDTO {
        return KafkaAktivitetskortWrapperDTO(
            aktivitetskortType,
            payload,
            source.name,
            messageId
        )
    }

    @JvmStatic
    fun aktivitetskortMelding(
        payload: Aktivitetskort,
        type: AktivitetskortType = AktivitetskortType.ARENA_TILTAK,
        source: MessageSource = MessageSource.ARENA_TILTAK_AKTIVITET_ACL): KafkaAktivitetskortWrapperDTO {
        return aktivitetskortMelding(payload, UUID.randomUUID(), type, source)
    }
}
