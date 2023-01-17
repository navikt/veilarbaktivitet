package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitetskort.test.AktivitetskortTestMetrikker;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    private final AktivitetskortTestMetrikker aktivitetskortTestMetrikker;

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

    public Optional<OppfolgingPeriodeMinimalDTO> finnOppfolgingsperiode(Person.AktorId aktorId, LocalDateTime opprettetTidspunkt, LocalDate startDato, LocalDate sluttDato) {
        var oppfolgingsperioderDTO = oppfolgingV2Client.hentOppfolgingsperioder(aktorId);

        if (oppfolgingsperioderDTO.isEmpty()) {
            aktivitetskortTestMetrikker.countFinnOppfolgingsperiode(5);

            log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE case 5 (bruker har ingen oppfølgingsperioder) - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}",
                    aktorId.get(),
                    opprettetTidspunkt,
                    startDato,
                    sluttDato,
                    List.of());

            return Optional.empty();
        }

        List<OppfolgingPeriodeMinimalDTO> oppfolgingsperioder = oppfolgingsperioderDTO.get();

        if (oppfolgingsperioder.isEmpty()) {
            aktivitetskortTestMetrikker.countFinnOppfolgingsperiode(5);

            log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE case 5 (bruker har ingen oppfølgingsperioder) - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}",
                    aktorId.get(),
                    opprettetTidspunkt,
                    startDato,
                    sluttDato,
                    List.of());

            return Optional.empty();
        }

        List<OppfolgingPeriodeMinimalDTO> oppfolgingsperioderCopy = new ArrayList<>(oppfolgingsperioder);
        oppfolgingsperioderCopy.sort(comparing(OppfolgingPeriodeMinimalDTO::getStartDato).reversed()); // nyeste først

        var opprettetTidspunktCZDT = ChronoZonedDateTime.from(opprettetTidspunkt.atZone(ZoneId.systemDefault()));
        var maybePerioder = oppfolgingsperioderCopy
                .stream()
                .filter(o -> {
                    var gjeldendePeriodePredikat = (o.getStartDato().isBefore(opprettetTidspunktCZDT) || o.getStartDato().isEqual(opprettetTidspunktCZDT)) && o.getSluttDato() == null;
                    if (gjeldendePeriodePredikat) {
                        aktivitetskortTestMetrikker.countFinnOppfolgingsperiode(1);
                        return true;
                    }
                    var gammelPeriodePredikat = (o.getStartDato().isBefore(opprettetTidspunktCZDT) || o.getStartDato().isEqual(opprettetTidspunktCZDT)) && o.getSluttDato().isAfter(opprettetTidspunktCZDT);
                    if (gammelPeriodePredikat) {
                        aktivitetskortTestMetrikker.countFinnOppfolgingsperiode(2);
                        return true;
                    }
                    return false;
                })
                .toList();

        if (maybePerioder.size() > 1) {
            aktivitetskortTestMetrikker.countFinnOppfolgingsperiode(3);

            log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE case 3 (flere matchende perioder) - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}",
                    aktorId.get(),
                    opprettetTidspunkt,
                    startDato,
                    sluttDato,
                    oppfolgingsperioder);
        }

        var maybePeriode = maybePerioder.stream().findFirst();

        return Optional.ofNullable(maybePeriode.orElseGet(() -> oppfolgingsperioderCopy
                .stream()
                .filter(o -> o.getSluttDato().isAfter(opprettetTidspunktCZDT))
                .min(comparingLong(o -> Math.abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, o.getStartDato())))) // filteret over kan returnere flere perioder, velg perioden som har startdato nærmest opprettettidspunkt
                .filter(o -> {
                    var innenTiMinutter = Math.abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, o.getStartDato())) < 600000;
                    if (innenTiMinutter) {
                        aktivitetskortTestMetrikker.countFinnOppfolgingsperiode(7);

                        log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE case 7 (startdato innen 10 minutter) - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}",
                                aktorId.get(),
                                opprettetTidspunkt,
                                startDato,
                                sluttDato,
                                oppfolgingsperioder);
                    } else {
                        aktivitetskortTestMetrikker.countFinnOppfolgingsperiode(4);

                        log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE case 4 (opprettetTidspunkt har ingen god match) - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}",
                                aktorId.get(),
                                opprettetTidspunkt,
                                startDato,
                                sluttDato,
                                oppfolgingsperioder);
                    }

                    return innenTiMinutter;
                })
                .orElse(null)
        ));
    }
}
