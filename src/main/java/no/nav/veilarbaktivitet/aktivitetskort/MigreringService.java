package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.RequiredArgsConstructor;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;

import static no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO.*;
import static no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO.GRUPPEAKTIVITET;

@Service
@RequiredArgsConstructor
public class MigreringService {
    public static final String EKSTERN_AKTIVITET_TOGGLE = "veilarbaktivitet.vis_eksterne_aktiviteter";
    private final UnleashClient unleashClient;

    private Predicate<ArenaAktivitetDTO> ikkeArenaTiltak = a -> List.of(GRUPPEAKTIVITET, UTDANNINGSAKTIVITET).contains(a.getType());
    private Predicate<ArenaAktivitetDTO> alleArenaAktiviteter = a -> true;
    public Predicate<ArenaAktivitetDTO> filtrerBortArenaTiltakHvisToggleAktiv() {
        if (unleashClient.isEnabled(EKSTERN_AKTIVITET_TOGGLE)) {
            return ikkeArenaTiltak;
        } else {
            return alleArenaAktiviteter;
        }
    }

    private Predicate<AktivitetDTO> ikkeEksterneAktiviteter = a -> AktivitetTypeDTO.TILTAKSAKTIVITET != a.getType();
    private Predicate<AktivitetDTO> alleLokaleAktiviteter = a -> true;

    public Predicate<AktivitetDTO> ikkeFiltrerBortEksterneAktiviteterHvisToggleAktiv() {
        if (unleashClient.isEnabled(EKSTERN_AKTIVITET_TOGGLE)) {
            return alleLokaleAktiviteter;
        } else {
            return ikkeEksterneAktiviteter;
        }
    }
}
