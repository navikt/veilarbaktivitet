package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.Ident
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
        payload: Aktivitetskort?,
        messageId: UUID?,
        source: String?,
        aktivitetskortType: AktivitetskortType?
    ): KafkaAktivitetskortWrapperDTO {
        return KafkaAktivitetskortWrapperDTO(
            aktivitetskortType!!,
            payload!!,
            source!!,
            messageId
        )
    }

    fun aktivitetskortMelding(
        payload: Aktivitetskort?,
        type: AktivitetskortType?,
        source: String?
    ): KafkaAktivitetskortWrapperDTO {
        return aktivitetskortMelding(payload, UUID.randomUUID(), source, type)
    }

    @JvmStatic
    fun aktivitetskortMelding(payload: Aktivitetskort?, type: AktivitetskortType?): KafkaAktivitetskortWrapperDTO {
        return aktivitetskortMelding(payload, UUID.randomUUID(), MessageSource.ARENA_TILTAK_AKTIVITET_ACL.name, type)
    }
}
