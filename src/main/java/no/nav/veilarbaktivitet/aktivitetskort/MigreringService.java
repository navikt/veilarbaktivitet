package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;

import static no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO.GRUPPEAKTIVITET;
import static no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO.UTDANNINGSAKTIVITET;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigreringService {
    public static final String VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE = "veilarbaktivitet.vis_migrerte_arena_aktiviteter";

    private final UnleashClient unleashClient;

    private final Predicate<ArenaAktivitetDTO> ikkeArenaTiltak = a -> List.of(GRUPPEAKTIVITET, UTDANNINGSAKTIVITET).contains(a.getType());
    private static final Predicate<ArenaAktivitetDTO> alleArenaAktiviteter = a -> true;
    public Predicate<ArenaAktivitetDTO> filtrerBortArenaTiltakHvisToggleAktiv() {
        if (unleashClient.isEnabled(VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)) {
            return ikkeArenaTiltak;
        } else {
            return alleArenaAktiviteter;
        }
    }

    private final Predicate<AktivitetDTO> ikkeMigrerteArenaAktiviteter = a -> !(AktivitetTypeDTO.EKSTERNAKTIVITET == a.getType() && AktivitetskortType.ARENA_TILTAK == a.getEksternAktivitet().type());
    private static final Predicate<AktivitetDTO> alleLokaleAktiviteter = a -> true;

    public Predicate<AktivitetDTO> visMigrerteArenaAktiviteterHvisToggleAktiv() {
        if (unleashClient.isEnabled(VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)) {
            return alleLokaleAktiviteter;
        } else {
            return ikkeMigrerteArenaAktiviteter;
        }
    }
}
