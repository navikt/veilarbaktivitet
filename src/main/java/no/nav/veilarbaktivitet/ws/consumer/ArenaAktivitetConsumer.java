package no.nav.veilarbaktivitet.ws.consumer;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.HentTiltakOgAktiviteterForBrukerPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.HentTiltakOgAktiviteterForBrukerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.HentTiltakOgAktiviteterForBrukerUgyldigInput;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerRequest;
import no.nav.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetTypeDTO;
import no.nav.veilarbaktivitet.domain.arena.ArenaStatusDTO;
import no.nav.veilarbaktivitet.domain.arena.MoteplanDTO;
import no.nav.veilarbaktivitet.util.DateUtils;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.time.ZoneId.systemDefault;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nav.veilarbaktivitet.domain.AktivitetStatus.*;

@Slf4j
@Component
public class ArenaAktivitetConsumer {

    private static final String ARENA_PREFIX = "ARENA";

    private final TiltakOgAktivitetV1 tiltakOgAktivitetV1;

    @Autowired
    ArenaAktivitetConsumer(TiltakOgAktivitetV1 tiltakOgAktivitetV1) {
        this.tiltakOgAktivitetV1 = tiltakOgAktivitetV1;
    }

    public List<ArenaAktivitetDTO> hentArenaAktiviteter(Person.Fnr personident) {

        val req = new HentTiltakOgAktiviteterForBrukerRequest();
        req.setPersonident(personident.get());
        List<ArenaAktivitetDTO> result = new ArrayList<>();

        try {
            val aktiviteter = tiltakOgAktivitetV1.hentTiltakOgAktiviteterForBruker(req);
            Optional.ofNullable(aktiviteter.getTiltaksaktivitetListe()).ifPresent((tiltakList) ->
                    result.addAll(tiltakList.stream()
                            .map(this::mapTilAktivitet)
                            .collect(toList())));
            Optional.ofNullable(aktiviteter.getGruppeaktivitetListe()).ifPresent((gruppeList) ->
                    result.addAll(gruppeList.stream()
                            .map(this::mapTilAktivitet)
                            .collect(toList())));
            Optional.ofNullable(aktiviteter.getUtdanningsaktivitetListe()).ifPresent((utdanningList) ->
                    result.addAll(utdanningList.stream()
                            .map(this::mapTilAktivitet)
                            .collect(toList())));
            return result;
        } catch (HentTiltakOgAktiviteterForBrukerPersonIkkeFunnet |
                HentTiltakOgAktiviteterForBrukerSikkerhetsbegrensning |
                HentTiltakOgAktiviteterForBrukerUgyldigInput e) {
            log.warn("Klarte ikke hente aktiviteter fra Arena.", e);
            return emptyList();
        }
    }

    private static final String VANLIG_AMO_NAVN = "Arbeidsmarkedsopplæring (AMO)";
    private static final String JOBBKLUBB_NAVN = "Jobbklubb";
    private static final String GRUPPE_AMO_NAVN = "Gruppe AMO";


    private String getTittel(Tiltaksaktivitet tiltaksaktivitet){
        val erVanligAmo = tiltaksaktivitet.getTiltaksnavn().trim()
                .equalsIgnoreCase(VANLIG_AMO_NAVN);
        if (erVanligAmo) {
            return "AMO-kurs: " + tiltaksaktivitet.getTiltakLokaltNavn();
        }

        val erGruppeAmo = tiltaksaktivitet.getTiltaksnavn().trim()
                .equalsIgnoreCase(GRUPPE_AMO_NAVN);
        if (erGruppeAmo){
            return "Gruppe AMO: " + tiltaksaktivitet.getTiltakLokaltNavn();
        }

        return tiltaksaktivitet.getTiltaksnavn();
    }

    private ArenaAktivitetDTO mapTilAktivitet(Tiltaksaktivitet tiltaksaktivitet) {
        val arenaAktivitetDTO = new ArenaAktivitetDTO()
                .setId(prefixArenaId(tiltaksaktivitet.getAktivitetId()))
                .setStatus(EnumUtils.valueOf(ArenaStatus.class, tiltaksaktivitet.getDeltakerStatus().getValue()).getStatus())
                .setType(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET)
                .setFraDato(mapPeriodeToDate(tiltaksaktivitet.getDeltakelsePeriode(), Periode::getFom))
                .setTilDato(mapPeriodeToDate(tiltaksaktivitet.getDeltakelsePeriode(), Periode::getTom))
                .setAvtalt(true)
                .setDeltakelseProsent(tiltaksaktivitet.getDeltakelseProsent())
                .setTiltaksnavn(tiltaksaktivitet.getTiltaksnavn())
                .setTiltakLokaltNavn(tiltaksaktivitet.getTiltakLokaltNavn())
                .setArrangoer(tiltaksaktivitet.getArrangoer())
                .setBedriftsnummer(tiltaksaktivitet.getBedriftsnummer())
                .setAntallDagerPerUke(tiltaksaktivitet.getAntallDagerPerUke())
                .setStatusSistEndret(DateUtils.getDate(tiltaksaktivitet.getStatusSistEndret()))
                .setOpprettetDato(DateUtils.getDate(tiltaksaktivitet.getStatusSistEndret()));

        val erVanligAmo = tiltaksaktivitet.getTiltaksnavn().trim()
                .equalsIgnoreCase(VANLIG_AMO_NAVN);

        arenaAktivitetDTO.setTittel(getTittel(tiltaksaktivitet));

        val erJobbKlubb = tiltaksaktivitet.getTiltaksnavn().trim()
                .equalsIgnoreCase(JOBBKLUBB_NAVN);

        if (erJobbKlubb) {
            arenaAktivitetDTO.setBeskrivelse(tiltaksaktivitet.getTiltakLokaltNavn());
        }

        val arenaEtikett = EnumUtils.valueOf(ArenaStatusDTO.class,
                tiltaksaktivitet.getDeltakerStatus().getValue());

        if (ArenaStatusDTO.TILBUD.equals(arenaEtikett)) {
            if (erJobbKlubb || erVanligAmo) {
                arenaAktivitetDTO.setEtikett(ArenaStatusDTO.TILBUD);
            }
        } else {
            arenaAktivitetDTO.setEtikett(arenaEtikett);
        }


        return arenaAktivitetDTO;
    }

    private ArenaAktivitetDTO mapTilAktivitet(Gruppeaktivitet gruppeaktivitet) {
        List<MoteplanDTO> motePlan = new ArrayList<>();
        Optional.ofNullable(gruppeaktivitet.getMoeteplanListe())
                .ifPresent(moeteplanListe -> moeteplanListe.stream()
                        .map(this::mapTilMoteplan)
                        .forEach(motePlan::add)
                );

        ZonedDateTime startDato = motePlan.get(0).getStartDato();
        ZonedDateTime sluttDato = motePlan.get(motePlan.size() - 1).getSluttDato();
        AktivitetStatus status = gruppeaktivitet.getStatus().getValue().equals("AVBR") ?
                AVBRUTT : mapTilAktivitetsStatus(startDato, sluttDato);
        return new ArenaAktivitetDTO()
                .setId(prefixArenaId(gruppeaktivitet.getAktivitetId()))
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

    private ArenaAktivitetDTO mapTilAktivitet(Utdanningsaktivitet utdanningsaktivitet) {
        ZonedDateTime startDato = DateUtils.getDate(utdanningsaktivitet.getAktivitetPeriode().getFom());
        ZonedDateTime sluttDato = DateUtils.getDate(utdanningsaktivitet.getAktivitetPeriode().getTom());

        return new ArenaAktivitetDTO()
                .setId(prefixArenaId(utdanningsaktivitet.getAktivitetId()))
                .setStatus(mapTilAktivitetsStatus(startDato, sluttDato))
                .setType(ArenaAktivitetTypeDTO.UTDANNINGSAKTIVITET)
                .setTittel(utdanningsaktivitet.getAktivitetstype())
                .setBeskrivelse(utdanningsaktivitet.getBeskrivelse())
                .setFraDato(startDato)
                .setTilDato(sluttDato)
                .setOpprettetDato(startDato)
                .setAvtalt(true);
    }

    private String prefixArenaId(String arenaId) {
        return ARENA_PREFIX + arenaId;
    }

    private AktivitetStatus mapTilAktivitetsStatus(ZonedDateTime startDato, ZonedDateTime sluttDato) {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime startOfDay = ofInstant(startDato.toInstant(), systemDefault()).toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = ofInstant(sluttDato.toInstant(), systemDefault()).toLocalDate().plusDays(1).atStartOfDay();

        return now.isBefore(startOfDay) ? PLANLAGT : now.isBefore(endOfDay) ? GJENNOMFORES : FULLFORT;
    }

    private MoteplanDTO mapTilMoteplan(Moeteplan moeteplan) {
        return new MoteplanDTO()
                .setSted(moeteplan.getSted())
                .setStartDato(DateUtils.getDate(DateUtils.mergeDateTime(moeteplan.getStartDato(), moeteplan.getStartKlokkeslett())))
                .setSluttDato(DateUtils.getDate(DateUtils.mergeDateTime(moeteplan.getSluttDato(), moeteplan.getSluttKlokkeslett())));
    }

    private ZonedDateTime mapPeriodeToDate(Periode date, Function<Periode, XMLGregorianCalendar> periodeDate) {
        return Optional.ofNullable(date).map(periodeDate).map(DateUtils::getDate).orElse(null);
    }

    private enum ArenaStatus {
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

        private AktivitetStatus status;

        AktivitetStatus getStatus() {
            return status;
        }
    }

}
