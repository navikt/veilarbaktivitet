package no.nav.veilarbaktivitet.aktivitetskort

import io.getunleash.Unleash
import lombok.extern.slf4j.Slf4j
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO
import no.nav.veilarbaktivitet.arena.model.ArenaId
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

    /* Hører kanskje ikke til her men var lettere å gjøre groupBy i kotlin vs java */
    fun countArenaAktiviteter(foer: MutableList<ArenaAktivitetDTO>, etter: MutableList<ArenaAktivitetDTO> ) {
        if (foer.isEmpty()) return
        val antallFoer = foer.groupBy { it.type }.mapValues { it.value.size }
        val antallEtter = etter.groupBy { it.type }.mapValues { it.value.size }
        ArenaAktivitetTypeDTO.values()
            .map {
                val totaltFoer = antallFoer[it] ?: 0
                val totaltEtter = antallEtter[it] ?: 0
                reportMetric(it, totaltFoer, totaltEtter)
            }
    }
    private fun reportMetric(type: ArenaAktivitetTypeDTO, foer: Int, etter: Int) {
        if (foer == 0) return
        aktivitetskortMetrikker.countMigrerteArenaAktiviteter(type, foer, etter)
    }

    fun filtrerBortArenaTiltakHvisToggleAktiv(arenaIds: Set<ArenaId?>): Predicate<ArenaAktivitetDTO> {
        return if (unleash.isEnabled(VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)) {
            // Hvis migrert, skjul fra /tiltak endepunkt
            Predicate { arenaAktivitetDTO: ArenaAktivitetDTO ->
                /* Fjern "historiserte" arena-aktiviteter ref
                * https://confluence.adeo.no/pages/viewpage.action?pageId=414017745 */
                arenaAktivitetDTO.id.startsWith("ARENATAH") ||
                    !arenaIds.contains(ArenaId(arenaAktivitetDTO.id))
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
