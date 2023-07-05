package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigreringService {
    public static final String VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE = "veilarbaktivitet.vis_migrerte_arena_aktiviteter";

    private final UnleashClient unleashClient;
    private final IdMappingDAO idMappingDAO;
    private static final Predicate<ArenaAktivitetDTO> alleArenaAktiviteter = a -> true;
    public Predicate<ArenaAktivitetDTO> filtrerBortArenaTiltakHvisToggleAktiv(Set<ArenaId> arenaIds) {
        if (unleashClient.isEnabled(VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)) {
            // Hvis migrert, skjul fra /tiltak endepunkt
            return arenaAktivitetDTO -> !arenaIds.contains(new ArenaId(arenaAktivitetDTO.getId()));
        } else {
            return alleArenaAktiviteter;
        }
    }
    public List<AktivitetDTO> visMigrerteArenaAktiviteterHvisToggleAktiv(List<AktivitetDTO> aktiviteter) {
        if (unleashClient.isEnabled(VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)) {
            return aktiviteter;
        } else {
            // Ikke vis migrerte aktiviter
            var funksjonelleIds = aktiviteter.stream().map(AktivitetDTO::getFunksjonellId)
                    .filter(Objects::nonNull)
                    .toList();
            var idMapping = idMappingDAO.getMappingsByFunksjonellId(funksjonelleIds);
            return aktiviteter.stream().filter(aktivitet -> !idMapping.containsKey(aktivitet.getFunksjonellId())).toList();
        }
    }
}
