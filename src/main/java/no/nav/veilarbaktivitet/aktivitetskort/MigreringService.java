package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.RequiredArgsConstructor;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingLong;
import static no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO.GRUPPEAKTIVITET;
import static no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO.UTDANNINGSAKTIVITET;

@Service
@RequiredArgsConstructor
public class MigreringService {
    public static final String EKSTERN_AKTIVITET_TOGGLE = "veilarbaktivitet.vis_eksterne_aktiviteter";

    private final UnleashClient unleashClient;

    private final OppfolgingV2Client oppfolgingV2Client;

    private Predicate<ArenaAktivitetDTO> ikkeArenaTiltak = a -> List.of(GRUPPEAKTIVITET, UTDANNINGSAKTIVITET).contains(a.getType());
    private Predicate<ArenaAktivitetDTO> alleArenaAktiviteter = a -> true;
    public Predicate<ArenaAktivitetDTO> filtrerBortArenaTiltakHvisToggleAktiv() {
        if (unleashClient.isEnabled(EKSTERN_AKTIVITET_TOGGLE)) {
            return ikkeArenaTiltak;
        } else {
            return alleArenaAktiviteter;
        }
    }

    private Predicate<AktivitetDTO> ikkeEksterneAktiviteter = a -> AktivitetTypeDTO.EKSTERNAKTIVITET != a.getType();
    private Predicate<AktivitetDTO> alleLokaleAktiviteter = a -> true;

    public Predicate<AktivitetDTO> ikkeFiltrerBortEksterneAktiviteterHvisToggleAktiv() {
        if (unleashClient.isEnabled(EKSTERN_AKTIVITET_TOGGLE)) {
            return alleLokaleAktiviteter;
        } else {
            return ikkeEksterneAktiviteter;
        }
    }

    public Optional<OppfolgingPeriodeMinimalDTO> finnOppfolgingsperiode(Person.AktorId aktorId, LocalDateTime opprettetTidspunkt) {
        var oppfolgingsperioderDTO = oppfolgingV2Client.hentOppfolgingsperioder(aktorId);

        if (oppfolgingsperioderDTO.isEmpty()) {
            return Optional.empty();
        }

        List<OppfolgingPeriodeMinimalDTO> oppfolgingsperioder = oppfolgingsperioderDTO.get();

        List<OppfolgingPeriodeMinimalDTO> oppfolgingsperioderCopy = new ArrayList<>(oppfolgingsperioder);
        oppfolgingsperioderCopy.sort(comparing(OppfolgingPeriodeMinimalDTO::getStartDato).reversed());

        var opprettetTidspunktCZDT = ChronoZonedDateTime.from(opprettetTidspunkt.atZone(ZoneId.systemDefault()));
        Optional<OppfolgingPeriodeMinimalDTO> maybePeriode = oppfolgingsperioderCopy
                .stream()
                .filter(o -> (o.getStartDato().isBefore(opprettetTidspunktCZDT) && o.getSluttDato() == null) ||
                        (o.getStartDato().isBefore(opprettetTidspunktCZDT) && o.getSluttDato().isAfter(opprettetTidspunktCZDT))
                )
                .findFirst();

        return Optional.of(maybePeriode.orElseGet(() -> oppfolgingsperioderCopy
                .stream()
                .min(comparingLong(o -> Math.abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, o.getStartDato()))))
                .orElseThrow(IllegalStateException::new)
        ));
    }
}
