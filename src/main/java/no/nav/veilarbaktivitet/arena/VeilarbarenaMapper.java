package no.nav.veilarbaktivitet.arena;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.arena.model.*;
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

@Slf4j
public class VeilarbarenaMapper {

    private static final String DATO_FORMAT = "yyyy-MM-dd";
    public static final String ARENA_PREFIX = "ARENA";

    static final String VANLIG_AMO_NAVN = "Arbeidsmarkedsopplæring (AMO)";
    static final String JOBBKLUBB_NAVN = "Jobbklubb";
    static final String GRUPPE_AMO_NAVN = "Gruppe AMO";
    static final String ENKELTPLASS_AMO_NAVN = "Enkeltplass AMO";

    private static final Date arenaAktivitetFilterDato = parseDato(getOptionalProperty(ARENA_AKTIVITET_DATOFILTER_PROPERTY).orElse(null));

    static Date parseDato(String konfigurertDato) {
        try {
            return new SimpleDateFormat(DATO_FORMAT).parse(konfigurertDato);
        } catch (Exception e) {
            log.warn("Kunne ikke parse dato [{}] med datoformat [{}].", konfigurertDato, DATO_FORMAT);
            return null;
        }
    }

    public static List<ArenaAktivitetDTO> map(AktiviteterDTO aktiviteter) {
        List<ArenaAktivitetDTO> result = new ArrayList<>();

        Optional.ofNullable(aktiviteter.getTiltaksaktiviteter()).ifPresent(tiltakList ->
                result.addAll(tiltakList.stream()
                        .map(VeilarbarenaMapper::mapTilAktivitet)
                        .toList()));
        Optional.ofNullable(aktiviteter.getGruppeaktiviteter()).ifPresent(gruppeList ->
                result.addAll(gruppeList.stream()
                        .map(VeilarbarenaMapper::mapTilAktivitet)
                        .toList()));
        Optional.ofNullable(aktiviteter.getUtdanningsaktiviteter()).ifPresent(utdanningList ->
                result.addAll(utdanningList.stream()
                        .map(VeilarbarenaMapper::mapTilAktivitet)
                        .toList()));
        return result.stream().filter(aktivitet -> etterFilterDato(aktivitet.getTilDato())).toList();
    }

    private static boolean etterFilterDato(Date tilDato) {
        return tilDato == null || arenaAktivitetFilterDato == null || arenaAktivitetFilterDato.before(tilDato);
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

    private static ArenaAktivitetDTO mapTilAktivitet(AktiviteterDTO.Tiltaksaktivitet tiltaksaktivitet) {
        val arenaAktivitetDTO = new ArenaAktivitetDTO()
                .setId(tiltaksaktivitet.getAktivitetId().id())
                .setStatus(EnumUtils.valueOf(ArenaStatus.class, tiltaksaktivitet.getDeltakerStatus()).getStatus())
                .setType(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET)
                .setFraDato(mapPeriodeToDate(tiltaksaktivitet.getDeltakelsePeriode(), AktiviteterDTO.Tiltaksaktivitet.DeltakelsesPeriode::getFom))
                .setTilDato(mapPeriodeToDate(tiltaksaktivitet.getDeltakelsePeriode(), AktiviteterDTO.Tiltaksaktivitet.DeltakelsesPeriode::getTom))
                .setAvtalt(true)
                .setDeltakelseProsent(tiltaksaktivitet.getDeltakelseProsent() != null ? tiltaksaktivitet.getDeltakelseProsent().floatValue() : null)
                .setTiltaksnavn(tiltaksaktivitet.getTiltaksnavn())
                .setTiltakLokaltNavn(tiltaksaktivitet.getTiltakLokaltNavn())
                .setArrangoer(tiltaksaktivitet.getArrangor())
                .setBedriftsnummer(tiltaksaktivitet.getBedriftsnummer())
                .setAntallDagerPerUke(tiltaksaktivitet.getAntallDagerPerUke())
                .setStatusSistEndret(mapToDate(tiltaksaktivitet.getStatusSistEndret()))
                .setOpprettetDato(mapToDate(tiltaksaktivitet.getStatusSistEndret()));

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

    private static ArenaAktivitetDTO mapTilAktivitet(AktiviteterDTO.Gruppeaktivitet gruppeaktivitet) {
        List<MoteplanDTO> motePlan = new ArrayList<>();
        Optional.ofNullable(gruppeaktivitet.getMoteplanListe())
                .ifPresent(moeteplanListe -> moeteplanListe.stream()
                        .map(VeilarbarenaMapper::mapTilMoteplan)
                        .forEach(motePlan::add)
                );

        Date startDato = motePlan.get(0).getStartDato();
        Date sluttDato = motePlan.get(motePlan.size() - 1).getSluttDato();
        AktivitetStatus status = "AVBR".equals(gruppeaktivitet.getStatus()) ?
                AVBRUTT : mapTilAktivitetsStatus(startDato, sluttDato);
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
                .setMoeteplanListe(motePlan);
    }

    private static ArenaAktivitetDTO mapTilAktivitet(AktiviteterDTO.Utdanningsaktivitet utdanningsaktivitet) {
        Date startDato = mapToDate(utdanningsaktivitet.getAktivitetPeriode().getFom());
        Date sluttDato = mapToDate(utdanningsaktivitet.getAktivitetPeriode().getTom());

        return new ArenaAktivitetDTO()
                .setId(utdanningsaktivitet.getAktivitetId().id())
                .setStatus(mapTilAktivitetsStatus(startDato, sluttDato))
                .setType(ArenaAktivitetTypeDTO.UTDANNINGSAKTIVITET)
                .setTittel(utdanningsaktivitet.getAktivitetstype())
                .setBeskrivelse(utdanningsaktivitet.getBeskrivelse())
                .setFraDato(startDato)
                .setTilDato(sluttDato)
                .setOpprettetDato(startDato)
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
            return DateUtils.toDate(localDate);
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

    enum ArenaStatus {
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
