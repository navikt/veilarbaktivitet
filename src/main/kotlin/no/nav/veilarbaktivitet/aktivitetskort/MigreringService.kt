package no.nav.veilarbaktivitet.aktivitetskort

import lombok.extern.slf4j.Slf4j
import no.nav.common.featuretoggle.UnleashClient
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaId
import org.springframework.stereotype.Service
import java.util.*
import java.util.function.Predicate

@Service
@Slf4j
class MigreringService (
    private val unleashClient: UnleashClient? = null,
    private val idMappingDAO: IdMappingDAO? = null
) {

    fun filtrerBortArenaTiltakHvisToggleAktiv(arenaIds: Set<ArenaId?>): Predicate<ArenaAktivitetDTO> {
        return if (unleashClient!!.isEnabled(VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)) {
            // Hvis migrert, skjul fra /tiltak endepunkt
            Predicate { arenaAktivitetDTO: ArenaAktivitetDTO -> !arenaIds.contains(ArenaId(arenaAktivitetDTO.id)) }
        } else {
            alleArenaAktiviteter
        }
    }

    fun visMigrerteArenaAktiviteterHvisToggleAktiv(aktiviteter: List<AktivitetDTO>): List<AktivitetDTO> {
        return if (unleashClient!!.isEnabled(VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)) {
            aktiviteter
        } else {
            // Ikke vis migrerte aktiviter
            val funksjonelleIds = aktiviteter.stream().map { obj: AktivitetDTO -> obj.funksjonellId }
                .filter { obj: UUID? -> Objects.nonNull(obj) }
                .toList()
            val idMapping = idMappingDAO!!.getMappingsByFunksjonellId(funksjonelleIds)
            aktiviteter.stream().filter { aktivitet: AktivitetDTO -> !idMapping.containsKey(aktivitet.funksjonellId) }
                .toList()
        }
    }

    companion object {
        const val VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE = "veilarbaktivitet.vis_migrerte_arena_aktiviteter"
        private val alleArenaAktiviteter = Predicate { a: ArenaAktivitetDTO -> true }
    }
}
