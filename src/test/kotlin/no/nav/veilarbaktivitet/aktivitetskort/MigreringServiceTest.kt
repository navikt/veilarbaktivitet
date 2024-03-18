package no.nav.veilarbaktivitet.aktivitetskort

import io.getunleash.Unleash
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingWithAktivitetStatus
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.oppfolging.periode.Oppfolgingsperiode
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import java.time.ZonedDateTime
import java.util.*

class MigreringServiceTest {

    val oppfolgingsperiode = Oppfolgingsperiode(
        "1",
        UUID.randomUUID(),
        ZonedDateTime.now(),
        ZonedDateTime.now()
    )
    val testAktiviteter: List<Pair<ArenaAktivitetDTO, Oppfolgingsperiode?>> = listOf(
        ArenaAktivitetDTO.builder()
            .type(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET)
            .id("ARENATA101")
            .status(AktivitetStatus.PLANLAGT).build() to oppfolgingsperiode,
        ArenaAktivitetDTO.builder()
            .type(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET)
            .id("ARENATA102")
            .status(AktivitetStatus.GJENNOMFORES).build() to oppfolgingsperiode,
        ArenaAktivitetDTO.builder()
            .type(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET)
            .id("ARENATA103")
            .status(AktivitetStatus.FULLFORT).build() to oppfolgingsperiode
    )

    @Test
    fun `skal telle feil status og riktig status`() {
        val metrikker = mock(AktivitetskortMetrikker::class.java)
        val migreringService = MigreringService(
            unleash = mock(Unleash::class.java),
            idMappingDAO = mock(IdMappingDAO::class.java),
            aktivitetskortMetrikker = metrikker
        )
        migreringService.countArenaAktiviteter(
            foer = testAktiviteter,
            idMappings = mapOf(
                ArenaId("ARENATA101") to IdMappingWithAktivitetStatus(
                    ArenaId("ARENATA101"),
                    1,
                    UUID.randomUUID(),
                    AktivitetStatus.PLANLAGT,
                    historiskDato = null,
                    source = MessageSource.ARENA_TILTAK_AKTIVITET_ACL
                ),
                ArenaId("ARENATA102") to IdMappingWithAktivitetStatus(
                    ArenaId("ARENATA102"),
                    2,
                    UUID.randomUUID(),
                    AktivitetStatus.PLANLAGT, // Feil status
                    historiskDato = null,
                    source = MessageSource.ARENA_TILTAK_AKTIVITET_ACL
                )
            )
        )
        verify(metrikker).countMigrerteArenaAktiviteter(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET, 3, 1,1,1, 0)
    }

    @Test
    fun `skal telle riktig når ingen er migrert`() {
        val metrikker = mock(AktivitetskortMetrikker::class.java)
        val migreringService = MigreringService(
            unleash = mock(Unleash::class.java),
            idMappingDAO = mock(IdMappingDAO::class.java),
            aktivitetskortMetrikker = metrikker
        )
        migreringService.countArenaAktiviteter(
            foer = testAktiviteter,
            idMappings = mapOf()
        )
        verify(metrikker).countMigrerteArenaAktiviteter(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET, 3, 0,0,3, 0)
    }

    @Test
    fun `skal telle riktig når alle er feil status`() {
        val metrikker = mock(AktivitetskortMetrikker::class.java)
        val migreringService = MigreringService(
            unleash = mock(Unleash::class.java),
            idMappingDAO = mock(IdMappingDAO::class.java),
            aktivitetskortMetrikker = metrikker
        )
        migreringService.countArenaAktiviteter(
            foer = testAktiviteter,
            idMappings = mapOf(
                ArenaId("ARENATA101") to IdMappingWithAktivitetStatus(
                    ArenaId("ARENATA101"),
                    1,
                    UUID.randomUUID(),
                    AktivitetStatus.FULLFORT,
                    historiskDato = null,
                    source = MessageSource.ARENA_TILTAK_AKTIVITET_ACL
                ),
                ArenaId("ARENATA102") to IdMappingWithAktivitetStatus(
                    ArenaId("ARENATA102"),
                    2,
                    UUID.randomUUID(),
                    AktivitetStatus.PLANLAGT, // Feil status
                    historiskDato = null,
                    source = MessageSource.ARENA_TILTAK_AKTIVITET_ACL
                ),
                ArenaId("ARENATA103") to IdMappingWithAktivitetStatus(
                    ArenaId("ARENATA103"),
                    3,
                    UUID.randomUUID(),
                    AktivitetStatus.PLANLAGT, // Feil status
                    historiskDato = null,
                    source = MessageSource.ARENA_TILTAK_AKTIVITET_ACL
                )
            )
        )
        verify(metrikker).countMigrerteArenaAktiviteter(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET, 3, 0,3,0, 0)
    }

    @Test
    fun `skal telle riktig når alle er aktiviteter ikke har oppfølgingsperiode`() {
        val metrikker = mock(AktivitetskortMetrikker::class.java)
        val migreringService = MigreringService(
            unleash = mock(Unleash::class.java),
            idMappingDAO = mock(IdMappingDAO::class.java),
            aktivitetskortMetrikker = metrikker
        )
        migreringService.countArenaAktiviteter(
            foer = testAktiviteter.let {
               listOf(
                   it[0],
                   it[1],
                   it[2].copy(second = null)
               )
            },
            idMappings = mapOf(
                ArenaId("ARENATA101") to IdMappingWithAktivitetStatus(
                    ArenaId("ARENATA101"),
                    1,
                    UUID.randomUUID(),
                    AktivitetStatus.FULLFORT,
                    historiskDato = null,
                    source = MessageSource.ARENA_TILTAK_AKTIVITET_ACL
                ),
                ArenaId("ARENATA102") to IdMappingWithAktivitetStatus(
                    ArenaId("ARENATA102"),
                    2,
                    UUID.randomUUID(),
                    AktivitetStatus.PLANLAGT, // Feil status
                    historiskDato = null,
                    source = MessageSource.ARENA_TILTAK_AKTIVITET_ACL
                ),
                ArenaId("ARENATA103") to IdMappingWithAktivitetStatus(
                    ArenaId("ARENATA103"),
                    3,
                    UUID.randomUUID(),
                    AktivitetStatus.PLANLAGT, // Feil status
                    historiskDato = null,
                    source = MessageSource.ARENA_TILTAK_AKTIVITET_ACL
                )
            )
        )
        verify(metrikker).countMigrerteArenaAktiviteter(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET, 3, 0,2,0, 1)
    }
}

