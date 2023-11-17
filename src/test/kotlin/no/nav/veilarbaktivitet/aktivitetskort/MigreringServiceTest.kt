package no.nav.veilarbaktivitet.aktivitetskort

import io.getunleash.Unleash
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingWithAktivitetStatus
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO
import no.nav.veilarbaktivitet.arena.model.ArenaId
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import java.util.*

class MigreringServiceTest {

    val testAktiviteter = listOf(
        ArenaAktivitetDTO.builder()
            .type(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET)
            .id("ARENATA101")
            .status(AktivitetStatus.PLANLAGT).build(),
        ArenaAktivitetDTO.builder()
            .type(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET)
            .id("ARENATA102")
            .status(AktivitetStatus.GJENNOMFORES).build(),
        ArenaAktivitetDTO.builder()
            .type(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET)
            .id("ARENATA103")
            .status(AktivitetStatus.FULLFORT).build()
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
                    historiskDato = null
                ),
                ArenaId("ARENATA102") to IdMappingWithAktivitetStatus(
                    ArenaId("ARENATA102"),
                    2,
                    UUID.randomUUID(),
                    AktivitetStatus.PLANLAGT, // Feil status
                    historiskDato = null
                )
            )
        )
        verify(metrikker).countMigrerteArenaAktiviteter(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET, 3, 1,1,1)
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
        verify(metrikker).countMigrerteArenaAktiviteter(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET, 3, 0,0,3)
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
                    historiskDato = null
                ),
                ArenaId("ARENATA102") to IdMappingWithAktivitetStatus(
                    ArenaId("ARENATA102"),
                    2,
                    UUID.randomUUID(),
                    AktivitetStatus.PLANLAGT, // Feil status
                    historiskDato = null
                ),
                ArenaId("ARENATA103") to IdMappingWithAktivitetStatus(
                    ArenaId("ARENATA103"),
                    3,
                    UUID.randomUUID(),
                    AktivitetStatus.PLANLAGT, // Feil status
                    historiskDato = null
                )
            )
        )
        verify(metrikker).countMigrerteArenaAktiviteter(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET, 3, 0,3,0)
    }
}

