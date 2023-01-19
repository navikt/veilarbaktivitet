package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class MigreringService {
    public static final String EKSTERN_AKTIVITET_TOGGLE = "veilarbaktivitet.vis_eksterne_aktiviteter";

    private final UnleashClient unleashClient;

    private final OppfolgingV2Client oppfolgingV2Client;

    private final Predicate<ArenaAktivitetDTO> ikkeArenaTiltak = a -> List.of(GRUPPEAKTIVITET, UTDANNINGSAKTIVITET).contains(a.getType());
    private static final Predicate<ArenaAktivitetDTO> alleArenaAktiviteter = a -> true;
    public Predicate<ArenaAktivitetDTO> filtrerBortArenaTiltakHvisToggleAktiv() {
        if (unleashClient.isEnabled(EKSTERN_AKTIVITET_TOGGLE)) {
            return ikkeArenaTiltak;
        } else {
            return alleArenaAktiviteter;
        }
    }

    private final Predicate<AktivitetDTO> ikkeEksterneAktiviteter = a -> AktivitetTypeDTO.EKSTERNAKTIVITET != a.getType();
    private static final Predicate<AktivitetDTO> alleLokaleAktiviteter = a -> true;

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
            log.info("Arenatiltak finn oppfølgingsperiode - bruker har ingen oppfølgingsperioder - aktorId={}, opprettetTidspunkt={}, oppfolgingsperioder={}",
                    aktorId.get(),
                    opprettetTidspunkt,
                    List.of());
            return Optional.empty();
        }

        List<OppfolgingPeriodeMinimalDTO> oppfolgingsperioder = oppfolgingsperioderDTO.get();

        if (oppfolgingsperioder.isEmpty()) {
            log.info("Arenatiltak finn oppfølgingsperiode - bruker har ingen oppfølgingsperioder - aktorId={}, opprettetTidspunkt={}, oppfolgingsperioder={}",
                    aktorId.get(),
                    opprettetTidspunkt,
                    List.of());
            return Optional.empty();
        }

        List<OppfolgingPeriodeMinimalDTO> oppfolgingsperioderCopy = new ArrayList<>(oppfolgingsperioder);
        oppfolgingsperioderCopy.sort(comparing(OppfolgingPeriodeMinimalDTO::getStartDato).reversed()); // nyeste først

        var opprettetTidspunktCZDT = ChronoZonedDateTime.from(opprettetTidspunkt.atZone(ZoneId.systemDefault()));
        var maybePeriode = oppfolgingsperioderCopy
                .stream()
                .filter(o -> ((o.getStartDato().isBefore(opprettetTidspunktCZDT) || o.getStartDato().isEqual(opprettetTidspunktCZDT)) && o.getSluttDato() == null) ||
                        ((o.getStartDato().isBefore(opprettetTidspunktCZDT) || o.getStartDato().isEqual(opprettetTidspunktCZDT)) && o.getSluttDato().isAfter(opprettetTidspunktCZDT)))
                .findFirst();

        return Optional.ofNullable(maybePeriode.orElseGet(() -> oppfolgingsperioderCopy
                .stream()
                .filter(o -> o.getSluttDato() == null || (o.getSluttDato().isAfter(opprettetTidspunktCZDT)))
                .min(comparingLong(o -> Math.abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, o.getStartDato())))) // filteret over kan returnere flere perioder, velg perioden som har startdato nærmest opprettettidspunkt
                .filter(o -> {
                    var innenTiMinutter = Math.abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, o.getStartDato())) < 600000;
                    if (innenTiMinutter) {
                        log.info("Arenatiltak finn oppfølgingsperiode - opprettetdato innen 10 minutter oppfølging startdato) - aktorId={}, opprettetTidspunkt={}, oppfolgingsperioder={}",
                                aktorId.get(),
                                opprettetTidspunkt,
                                oppfolgingsperioder);
                    }
                    return innenTiMinutter;
                }).orElseGet(() -> {
                    log.info("Arenatiltak finn oppfølgingsperiode - opprettetTidspunkt har ingen god match på oppfølgingsperioder) - aktorId={}, opprettetTidspunkt={}, oppfolgingsperioder={}",
                            aktorId.get(),
                            opprettetTidspunkt,
                            oppfolgingsperioder);
                    return null;
                })
        ));
    }
}
