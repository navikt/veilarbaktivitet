package no.nav.veilarbaktivitet.arena;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.arena.model.*;
import no.nav.veilarbaktivitet.oppfolging.periode.Oppfolgingsperiode;
import no.nav.veilarbaktivitet.util.DateUtils;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.time.ZoneId.systemDefault;
import static java.time.ZonedDateTime.ofInstant;
import static no.nav.common.utils.EnvironmentUtils.getOptionalProperty;
import static no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus.*;
import static no.nav.veilarbaktivitet.config.ApplicationContext.ARENA_AKTIVITET_DATOFILTER_PROPERTY;
import static no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeUtilKt.finnOppfolgingsperiodeForArenaAktivitet;
import static no.nav.veilarbaktivitet.util.DateUtils.toDate;
import static no.nav.veilarbaktivitet.util.DateUtils.toLocalDate;

@Slf4j
public class VeilarbarenaMapper {

    private static final String DATO_FORMAT = "yyyy-MM-dd";

    public static final String VANLIG_AMO_NAVN = "Arbeidsmarkedsopplæring (AMO)";
    public static final String JOBBKLUBB_NAVN = "Jobbklubb";
    public static final String GRUPPE_AMO_NAVN = "Gruppe AMO";
    public static final String ENKELTPLASS_AMO_NAVN = "Enkeltplass AMO";

    private static final Date arenaAktivitetFilterDato = parseDato(getOptionalProperty(ARENA_AKTIVITET_DATOFILTER_PROPERTY).orElse(null));

    static Date parseDato(String konfigurertDato) {
        try {
            return new SimpleDateFormat(DATO_FORMAT).parse(konfigurertDato);
        } catch (Exception e) {
            log.warn("Kunne ikke parse dato [{}] med datoformat [{}].", konfigurertDato, DATO_FORMAT);
            return null;
        }
    }

    public static List<ArenaAktivitetDTO> map(AktiviteterDTO aktiviteter, List<Oppfolgingsperiode> oppfolgingsperioder) {
        List<ArenaAktivitetDTO> result = new ArrayList<>();

        Optional.ofNullable(aktiviteter.getTiltaksaktiviteter()).ifPresent(tiltakList ->
                result.addAll(tiltakList.stream()
                        .map(aktivitet -> VeilarbarenaMapper.mapTilAktivitet(aktivitet, oppfolgingsperioder))
                        .toList()));
        Optional.ofNullable(aktiviteter.getGruppeaktiviteter()).ifPresent(gruppeList ->
                result.addAll(gruppeList.stream()
                        .map(aktivitet -> VeilarbarenaMapper.mapTilAktivitet(aktivitet, oppfolgingsperioder))
                        .toList()));
        Optional.ofNullable(aktiviteter.getUtdanningsaktiviteter()).ifPresent(utdanningList ->
                result.addAll(utdanningList.stream()
                        .map(aktivitet -> VeilarbarenaMapper.mapTilAktivitet(aktivitet, oppfolgingsperioder))
                        .toList()));
        return result.stream()
            .filter(aktivitet -> etterFilterDato(aktivitet.getTilDato()))
//            .filter(aktivitet -> etterFilterDato(aktivitet.getStatusSistEndret()))
            .toList();
    }

    private static boolean etterFilterDato(Date dato) {
        return dato == null || arenaAktivitetFilterDato == null || arenaAktivitetFilterDato.before(dato);
    }

    private static String getTittel(AktiviteterDTO.Tiltaksaktivitet tiltaksaktivitet){
        val erVanligAmo = tiltaksaktivitet.getTiltaksnavn().trim()
                .equalsIgnoreCase(VANLIG_AMO_NAVN);
        if (erVanligAmo) {
            return "AMO-kurs: " + tiltaksaktivitet.getTiltakLokaltNavn();
        }

        val erGruppeAmo = tiltaksaktivitet.getTiltaksnavn().trim()
                .equalsIgnoreCase(GRUPPE_AMO_NAVN);
        if (erGruppeAmo) {
            return "Gruppe AMO: " + tiltaksaktivitet.getTiltakLokaltNavn();
        }

        val erEnkeltplassAmo = tiltaksaktivitet.getTiltaksnavn().trim()
                .equalsIgnoreCase(ENKELTPLASS_AMO_NAVN);
        if (erEnkeltplassAmo) {
            return "Enkeltplass AMO: " + tiltaksaktivitet.getTiltakLokaltNavn();
        }

        return tiltaksaktivitet.getTiltaksnavn();
    }

    static ArenaAktivitetDTO mapTilAktivitet(AktiviteterDTO.Tiltaksaktivitet tiltaksaktivitet, List<Oppfolgingsperiode> oppfolgingsperioder) {
        val sistEndret = Optional.ofNullable(tiltaksaktivitet.getStatusSistEndret());
        val tilDatoDate = mapPeriodeToDate(tiltaksaktivitet.getDeltakelsePeriode(), AktiviteterDTO.Tiltaksaktivitet.DeltakelsesPeriode::getTom);
        val tilDato = tilDatoDate != null ? DateUtils.dateToLocalDate(tilDatoDate) : null;
        LocalDate oppslagsDato;
        if (sistEndret.isEmpty() && tilDato == null) {
            oppslagsDato = null;
        } else if (sistEndret.isPresent() && tilDato != null) {
            if (tilDato.isBefore(sistEndret.get())) {
                oppslagsDato = tilDato;
            } else {
                oppslagsDato = sistEndret.get();
            }
        } else if (sistEndret.isEmpty()) {
            oppslagsDato = tilDato;
        } else {
            oppslagsDato = sistEndret.get();
        }
        val oppfolgingsperiode = finnOppfolgingsperiodeForArenaAktivitet(oppfolgingsperioder, oppslagsDato);

        val arenaAktivitetDTO = new ArenaAktivitetDTO()
                .setId(tiltaksaktivitet.getAktivitetId().id())
                .setStatus(EnumUtils.valueOf(ArenaStatus.class, tiltaksaktivitet.getDeltakerStatus()).getStatus())
                .setType(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET)
                .setFraDato(mapPeriodeToDate(tiltaksaktivitet.getDeltakelsePeriode(), AktiviteterDTO.Tiltaksaktivitet.DeltakelsesPeriode::getFom))
                .setTilDato(tilDatoDate)
                .setAvtalt(true)
                .setDeltakelseProsent(tiltaksaktivitet.getDeltakelseProsent() != null ? tiltaksaktivitet.getDeltakelseProsent().floatValue() : null)
                .setTiltaksnavn(tiltaksaktivitet.getTiltaksnavn())
                .setTiltakLokaltNavn(tiltaksaktivitet.getTiltakLokaltNavn())
                .setArrangoer(tiltaksaktivitet.getArrangor())
                .setBedriftsnummer(tiltaksaktivitet.getBedriftsnummer())
                .setAntallDagerPerUke(tiltaksaktivitet.getAntallDagerPerUke())
                .setStatusSistEndret(mapToDate(sistEndret.orElse(null)))
                .setOpprettetDato(mapToDate(tiltaksaktivitet.getStatusSistEndret()))
                .setOppfolgingsperiodeId(oppfolgingsperiode != null ? oppfolgingsperiode.oppfolgingsperiodeId() : null);

        val erVanligAmo = tiltaksaktivitet.getTiltaksnavn().trim()
                .equalsIgnoreCase(VANLIG_AMO_NAVN);

        arenaAktivitetDTO.setTittel(getTittel(tiltaksaktivitet));

        val erJobbKlubb = tiltaksaktivitet.getTiltaksnavn().trim()
                .equalsIgnoreCase(JOBBKLUBB_NAVN);

        if (erJobbKlubb) {
            arenaAktivitetDTO.setBeskrivelse(tiltaksaktivitet.getTiltakLokaltNavn());
        }

        val arenaEtikett = EnumUtils.valueOf(ArenaStatusDTO.class,
                tiltaksaktivitet.getDeltakerStatus());

        if (ArenaStatusDTO.TILBUD.equals(arenaEtikett)) {
            if (erJobbKlubb || erVanligAmo) {
                arenaAktivitetDTO.setEtikett(ArenaStatusDTO.TILBUD);
            }
        } else {
            arenaAktivitetDTO.setEtikett(arenaEtikett);
        }

        return arenaAktivitetDTO;
    }

    static ArenaAktivitetDTO mapTilAktivitet(AktiviteterDTO.Gruppeaktivitet gruppeaktivitet, List<Oppfolgingsperiode> oppfolgingsperioder) {
        List<MoteplanDTO> motePlan = new ArrayList<>();
        Optional.ofNullable(gruppeaktivitet.getMoteplanListe())
                .ifPresent(moeteplanListe -> moeteplanListe.stream()
                        .map(VeilarbarenaMapper::mapTilMoteplan)
                        .forEach(motePlan::add)
                );

        Date startDato = motePlan.getFirst().getStartDato();
        Date sluttDato = motePlan.getLast().getSluttDato();
        AktivitetStatus status = "AVBR".equals(gruppeaktivitet.getStatus()) ?
                AVBRUTT : mapTilAktivitetsStatus(startDato, sluttDato);

        val oppfolgingsperiode = finnOppfolgingsperiodeForArenaAktivitet(oppfolgingsperioder, toLocalDate(startDato));

        return new ArenaAktivitetDTO()
                .setId(gruppeaktivitet.getAktivitetId().id())
                .setStatus(status)
                .setTittel(StringUtils.capitalize(gruppeaktivitet.getAktivitetstype()))
                .setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET)
                .setBeskrivelse(gruppeaktivitet.getBeskrivelse())
                .setFraDato(startDato)
                .setTilDato(sluttDato)
                .setAvtalt(true)
                .setOpprettetDato(startDato)
                .setOppfolgingsperiodeId(oppfolgingsperiode != null ? oppfolgingsperiode.oppfolgingsperiodeId() : null)
                .setMoeteplanListe(motePlan);
    }

    static ArenaAktivitetDTO mapTilAktivitet(AktiviteterDTO.Utdanningsaktivitet utdanningsaktivitet, List<Oppfolgingsperiode> oppfolgingsperioder) {
        Date startDato = mapToDate(utdanningsaktivitet.getAktivitetPeriode().getFom());
        Date sluttDato = mapToDate(utdanningsaktivitet.getAktivitetPeriode().getTom());
        val oppfolgingsperiode = finnOppfolgingsperiodeForArenaAktivitet(oppfolgingsperioder, toLocalDate(startDato));

        return new ArenaAktivitetDTO()
                .setId(utdanningsaktivitet.getAktivitetId().id())
                .setStatus(mapTilAktivitetsStatus(startDato, sluttDato))
                .setType(ArenaAktivitetTypeDTO.UTDANNINGSAKTIVITET)
                .setTittel(utdanningsaktivitet.getAktivitetstype())
                .setBeskrivelse(utdanningsaktivitet.getBeskrivelse())
                .setFraDato(startDato)
                .setTilDato(sluttDato)
                .setOpprettetDato(startDato)
                .setOppfolgingsperiodeId(oppfolgingsperiode != null ? oppfolgingsperiode.oppfolgingsperiodeId() : null)
                .setAvtalt(true);
    }


    private static AktivitetStatus mapTilAktivitetsStatus(Date startDato, Date sluttDato) {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime startDatoStart = ofInstant(startDato.toInstant(), systemDefault()).toLocalDate().atStartOfDay();
        LocalDateTime sluttDatoSlutt = ofInstant(sluttDato.toInstant(), systemDefault()).toLocalDate().plusDays(1).atStartOfDay();

        AktivitetStatus gjennomforesEllerFullfort = now.isBefore(sluttDatoSlutt) ? GJENNOMFORES : FULLFORT;
        return now.isBefore(startDatoStart) ? PLANLAGT : gjennomforesEllerFullfort;
    }

    private static MoteplanDTO mapTilMoteplan(AktiviteterDTO.Gruppeaktivitet.Moteplan moteplan) {
        return new MoteplanDTO()
                .setSted(moteplan.getSted())
                .setStartDato(mapDateTimeToDate(moteplan.getStartDato(), moteplan.getStartKlokkeslett()))
                .setSluttDato(mapDateTimeToDate(moteplan.getSluttDato(), moteplan.getSluttKlokkeslett()));
    }

    private static Date mapDateTimeToDate(LocalDate localDate, String klokkeslett) {
        if (klokkeslett == null) {
            return toDate(localDate);
        }
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime localTime = LocalTime.parse(klokkeslett, dateTimeFormatter);

        LocalDateTime localDateTime = localDate.atTime(localTime);
        return Date.from(localDateTime.atZone(systemDefault()).toInstant());
    }

    private static Date mapPeriodeToDate(AktiviteterDTO.Tiltaksaktivitet.DeltakelsesPeriode periode, Function<AktiviteterDTO.Tiltaksaktivitet.DeltakelsesPeriode, LocalDate> periodeDate) {
        return Optional.ofNullable(periode).map(periodeDate).map(DateUtils::toDate).orElse(null);
    }

    private static Date mapToDate(LocalDate localdate) {
        return Optional.ofNullable(localdate).map(DateUtils::toDate).orElse(null);
    }

    public enum ArenaStatus {
        AKTUELL(PLANLAGT),
        AVSLAG(AVBRUTT),
        DELAVB(AVBRUTT),
        FULLF(FULLFORT),
        GJENN(GJENNOMFORES),
        GJENN_AVB(AVBRUTT),
        GJENN_AVL(AVBRUTT),
        IKKAKTUELL(AVBRUTT),
        IKKEM(AVBRUTT),
        INFOMOETE(PLANLAGT),
        JATAKK(PLANLAGT),
        NEITAKK(AVBRUTT),
        TILBUD(PLANLAGT),
        VENTELISTE(PLANLAGT);

        ArenaStatus(AktivitetStatus status) {
            this.status = status;
        }

        private final AktivitetStatus status;

        AktivitetStatus getStatus() {
            return status;
        }
    }
}
