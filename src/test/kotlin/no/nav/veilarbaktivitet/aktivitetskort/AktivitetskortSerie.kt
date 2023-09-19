package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitetskort.dto.Aktivitetskort
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import java.time.ZonedDateTime
import java.util.*
import kotlin.random.Random

open class AktivitetskortSerie(
    val mockBruker: MockBruker,
    val type: AktivitetskortType = AktivitetskortType.VARIG_LONNSTILSKUDD
) {
    val funksjonellId = UUID.randomUUID()
    companion object {
        @JvmStatic
        fun of(mockBruker: MockBruker): AktivitetskortSerie {
            return AktivitetskortSerie(mockBruker)
        }
    }

    fun ny(
        status: AktivitetStatus = AktivitetStatus.GJENNOMFORES,
        endretTidspunkt: ZonedDateTime = ZonedDateTime.now()
    ): KafkaAktivitetskortWrapperDTO {
        return AktivitetskortUtil.aktivitetskortMelding(
            AktivitetskortUtil.ny(
                funksjonellId,
                status,
                endretTidspunkt,
                mockBruker
            ), type)
    }
}

class ArenaAktivitetskortSerie(mockBruker: MockBruker, val tiltakskode: String) {
    private val serie = AktivitetskortSerie(mockBruker, AktivitetskortType.ARENA_TILTAK)
    val funksjonellId = serie.funksjonellId
    val arenaId = ArenaId("TA" + Random.nextInt(1000, 10000))
    fun ny(status: AktivitetStatus, endretTidspunkt: ZonedDateTime): ArenaKort {
        return ArenaKort(serie.ny(status, endretTidspunkt), ArenaMeldingHeaders(arenaId, tiltakskode))
    }

    companion object {
        @JvmStatic
        fun of(mockBruker: MockBruker, tiltakskode: String): ArenaAktivitetskortSerie {
            return ArenaAktivitetskortSerie(mockBruker, tiltakskode)
        }
    }
}

class ArenaKort(
    val melding: KafkaAktivitetskortWrapperDTO,
    val header: ArenaMeldingHeaders
) {
   constructor(kort: Aktivitetskort, header: ArenaMeldingHeaders) : this(
       AktivitetskortUtil.aktivitetskortMelding(kort, AktivitetskortType.ARENA_TILTAK, AktivitetsbestillingCreator.ARENA_TILTAK_AKTIVITET_ACL),
       header
   )
}