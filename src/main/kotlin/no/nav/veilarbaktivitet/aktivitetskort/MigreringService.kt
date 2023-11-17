package no.nav.veilarbaktivitet.aktivitetskort

import io.getunleash.Unleash
import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingWithAktivitetStatus
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO
import no.nav.veilarbaktivitet.arena.model.ArenaId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.function.Predicate

@Service
@Slf4j
class MigreringService (
    private val unleash: Unleash,
    private val idMappingDAO: IdMappingDAO,
    private val aktivitetskortMetrikker: AktivitetskortMetrikker
) {
    val log = LoggerFactory.getLogger(javaClass)

    /* Hører kanskje ikke til her men var lettere å gjøre groupBy i kotlin vs java */
    fun countArenaAktiviteter(
        foer: List<ArenaAktivitetDTO>,
        idMappings: Map<ArenaId, IdMappingWithAktivitetStatus>
    ) {
        if (foer.isEmpty()) return
        val antallFoer = foer.groupBy { it.type }.mapValues { it.value.size }

        // Bruker status som indikator på om dataene er riktig
        fun sjekkMigreringsStatus(aktivitet: ArenaAktivitetDTO): MigreringsStatus {
            return idMappings[ArenaId(aktivitet.id)]
                ?.let { match -> if (match.status == aktivitet.status) MigreringsStatus.MigrertRiktigStatus else MigreringsStatus.MigrertFeilStatus  }
                ?: MigreringsStatus.IkkeMigrert
        }
        val migrert = foer
            .map { it to sjekkMigreringsStatus(it) }
            .filter { it.second != MigreringsStatus.IkkeMigrert }

        val (etterMedRiktigStatus, etterMedFeilStatus) = migrert.partition { it.second == MigreringsStatus.MigrertRiktigStatus }
        val antallMigrertMedRiktigStatus = etterMedRiktigStatus.groupBy { it.first.type }.mapValues { it.value.size }
        val antallMigrertMedFeilStatus = etterMedFeilStatus.groupBy { it.first.type }.mapValues { it.value.size }
        log.info("Migrerte aktiviteter med feil status: ${etterMedFeilStatus.joinToString(",") { it.first.id }}")
        ArenaAktivitetTypeDTO.values()
            .map {
                val totaltFoer = antallFoer[it] ?: 0
                val totaltMigrertRiktigStatus = antallMigrertMedRiktigStatus[it] ?: 0
                val totaltMigrertFeilStatus = antallMigrertMedFeilStatus[it] ?: 0
                val ikkeMigrert = totaltFoer - (totaltMigrertRiktigStatus + totaltMigrertFeilStatus)
                reportMetric(it, totaltFoer, totaltMigrertRiktigStatus, totaltMigrertFeilStatus, ikkeMigrert)
            }
    }
    private fun reportMetric(type: ArenaAktivitetTypeDTO, total: Int, migrertRiktigStatus: Int, migrertFeilStatus: Int, ikkeMigrert: Int) {
        if (total == 0) return
        aktivitetskortMetrikker.countMigrerteArenaAktiviteter(type, total, migrertRiktigStatus, migrertFeilStatus, ikkeMigrert)
    }

    fun filtrerBortArenaTiltakHvisToggleAktiv(arenaIds: Set<ArenaId?>): Predicate<ArenaAktivitetDTO> {
        return if (unleash.isEnabled(VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)) {
            // Hvis migrert, skjul fra /tiltak endepunkt
            Predicate { arenaAktivitetDTO: ArenaAktivitetDTO ->
                /* Fjern "historiserte" arena-aktiviteter ref
                * https://confluence.adeo.no/pages/viewpage.action?pageId=414017745 */
                val erHistorisertTiltak = arenaAktivitetDTO.id.startsWith("ARENATAH")
                val erMigrert = arenaIds.contains(ArenaId(arenaAktivitetDTO.id))
                val skalVises = !erHistorisertTiltak && !erMigrert
                skalVises
            }
        } else {
            alleArenaAktiviteter
        }
    }

    fun visMigrerteArenaAktiviteterHvisToggleAktiv(aktiviteter: List<AktivitetDTO>): List<AktivitetDTO> {
        return if (unleash.isEnabled(VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)) {
            aktiviteter
        } else {
            // Ikke vis migrerte aktiviter
            val funksjonelleIds = aktiviteter.stream().map { obj: AktivitetDTO -> obj.funksjonellId }
                .filter { obj: UUID? -> Objects.nonNull(obj) }
                .toList()
            val idMapping = idMappingDAO.getMappingsByFunksjonellId(funksjonelleIds)
            aktiviteter.stream().filter { aktivitet: AktivitetDTO -> !idMapping.containsKey(aktivitet.funksjonellId) }
                .toList()
        }
    }

    companion object {
        const val VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE = "veilarbaktivitet.vis_migrerte_arena_aktiviteter"
        private val alleArenaAktiviteter = Predicate { _: ArenaAktivitetDTO -> true }
    }
}

enum class MigreringsStatus {
    IkkeMigrert,
    MigrertRiktigStatus,
    MigrertFeilStatus
}
