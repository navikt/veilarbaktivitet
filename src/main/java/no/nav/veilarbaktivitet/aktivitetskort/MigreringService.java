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

import java.time.*;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.ChronoField;
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

    public Optional<OppfolgingPeriodeMinimalDTO> finnOppfolgingsperiodeMetrikker(Person.AktorId aktorId, LocalDateTime opprettetTidspunkt, LocalDate startDato, LocalDate sluttDato) {
        var oppfolgingsperioderDTO = oppfolgingV2Client.hentOppfolgingsperioder(aktorId);

        if (oppfolgingsperioderDTO.isEmpty()) {
            aktivitetskortTestMetrikker.countFinnOppfolgingsperiode("INGEN_PERIODE");

            log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE INGEN_PERIODE - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}",
                    aktorId.get(),
                    opprettetTidspunkt,
                    startDato,
                    sluttDato,
                    List.of());

            return Optional.empty();
        }

        List<OppfolgingPeriodeMinimalDTO> oppfolgingsperioder = oppfolgingsperioderDTO.get();

        if (oppfolgingsperioder.isEmpty()) {
            aktivitetskortTestMetrikker.countFinnOppfolgingsperiode("INGEN_PERIODE");

            log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE INGEN_PERIODE - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}",
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
                        aktivitetskortTestMetrikker.countFinnOppfolgingsperiode("GJELDENDE_PERIODE");
                        return true;
                    }
                    var gammelPeriodePredikat = (o.getStartDato().isBefore(opprettetTidspunktCZDT) || o.getStartDato().isEqual(opprettetTidspunktCZDT)) && o.getSluttDato().isAfter(opprettetTidspunktCZDT);
                    if (gammelPeriodePredikat) {
                        aktivitetskortTestMetrikker.countFinnOppfolgingsperiode("AVSLUTTET_PERIODE");
                        return true;
                    }
                    return false;
                })
                .toList();

        if (maybePerioder.size() > 1) {
            aktivitetskortTestMetrikker.countFinnOppfolgingsperiode("FLERE_MATCHENDE_PERIODER");

            log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE FLERE_MATCHENDE_PERIODER - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}",
                    aktorId.get(),
                    opprettetTidspunkt,
                    startDato,
                    sluttDato,
                    oppfolgingsperioder);
        }

        var maybePeriode = maybePerioder.stream().findFirst();

        return Optional.ofNullable(maybePeriode.orElseGet(() -> oppfolgingsperioderCopy
                .stream()
                .filter(o -> o.getSluttDato() == null || (o.getSluttDato().isAfter(opprettetTidspunktCZDT)))
                .min(comparingLong(o -> Math.abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, o.getStartDato())))) // filteret over kan returnere flere perioder, velg perioden som har startdato nærmest opprettettidspunkt
                .filter(o -> {
                    var innenTiMinutter = Math.abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, o.getStartDato())) < 1000 * 60 * 10;
                    var innenEnTime = Math.abs(ChronoUnit.MILLIS.between(opprettetTidspunktCZDT, o.getStartDato())) < 1000 * 60 * 60;
                    var sammeDag = opprettetTidspunktCZDT.toLocalDate().isEqual(o.getStartDato().toLocalDate());
                    var innenNesteVirkedag = getNextWorkingDay(opprettetTidspunkt).isAfter(o.getStartDato().toLocalDateTime());
                    var innenToVirkedager = getNextWorkingDay(getNextWorkingDay(opprettetTidspunkt)).isAfter(o.getStartDato().toLocalDateTime());
                    var innenEnUke = opprettetTidspunkt.plus(7, ChronoUnit.DAYS).isAfter(o.getStartDato().toLocalDateTime());

                    if (innenTiMinutter) {
                        aktivitetskortTestMetrikker.countFinnOppfolgingsperiode("INNEN_TI_MINUTTER");
                        log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE INNEN_TI_MINUTTER - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}", aktorId.get(), opprettetTidspunkt, startDato, sluttDato, oppfolgingsperioder);
                    }
                    if (innenEnTime) {
                        aktivitetskortTestMetrikker.countFinnOppfolgingsperiode("INNEN_EN_TIME");
                        log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE INNEN_EN_TIME - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}", aktorId.get(), opprettetTidspunkt, startDato, sluttDato, oppfolgingsperioder);
                    }
                    if (sammeDag) {
                        aktivitetskortTestMetrikker.countFinnOppfolgingsperiode("SAMME_DAG");
                        log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE SAMME_DAG - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}", aktorId.get(), opprettetTidspunkt, startDato, sluttDato, oppfolgingsperioder);
                    }
                    if (innenNesteVirkedag) {
                        aktivitetskortTestMetrikker.countFinnOppfolgingsperiode("INNEN_NESTE_VIRKEDAG");
                        log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE INNEN_NESTE_VIRKEDAG - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}", aktorId.get(), opprettetTidspunkt, startDato, sluttDato, oppfolgingsperioder);
                    }
                    if (innenToVirkedager) {
                        aktivitetskortTestMetrikker.countFinnOppfolgingsperiode("INNEN_TO_VIRKEDAGER");
                        log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE INNEN_TO_VIRKEDAGER - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}", aktorId.get(), opprettetTidspunkt, startDato, sluttDato, oppfolgingsperioder);
                    }
                    if (innenEnUke) {
                        aktivitetskortTestMetrikker.countFinnOppfolgingsperiode("INNEN_EN_UKE");
                        log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE INNEN_EN_UKE - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}", aktorId.get(), opprettetTidspunkt, startDato, sluttDato, oppfolgingsperioder);
                    }
                    return innenTiMinutter || innenEnTime || sammeDag || innenNesteVirkedag || innenToVirkedager || innenEnUke;
                })
                .orElseGet(() -> {
                    aktivitetskortTestMetrikker.countFinnOppfolgingsperiode("INGEN_GOD_MATCH");

                    log.info("MIGRERINGSERVICE.FINNOPPFOLGINGSPERIODE INGEN_GOD_MATCH - aktorId={}, opprettetTidspunkt={}, startDato={}, sluttDato={}, oppfolgingsperioder={}",
                            aktorId.get(),
                            opprettetTidspunkt,
                            startDato,
                            sluttDato,
                            oppfolgingsperioder);
                    return null;
                })
        ));
    }

    private static LocalDateTime getNextWorkingDay(LocalDateTime date) {
        DayOfWeek dayOfWeek = DayOfWeek.of(date.get(ChronoField.DAY_OF_WEEK));
        return switch (dayOfWeek) {
            case FRIDAY -> date.plus(3, ChronoUnit.DAYS);
            case SATURDAY -> date.plus(2, ChronoUnit.DAYS);
            default -> date.plus(1, ChronoUnit.DAYS);
        };
    }
}
