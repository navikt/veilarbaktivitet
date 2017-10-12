package no.nav.fo.veilarbaktivitet.ws.consumer;

import lombok.val;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaAktivitetTypeDTO;
import no.nav.fo.veilarbaktivitet.domain.arena.ArenaStatusDTO;
import no.nav.fo.veilarbaktivitet.domain.arena.MoteplanDTO;
import no.nav.fo.veilarbaktivitet.util.DateUtils;
import no.nav.fo.veilarbaktivitet.util.EnumUtils;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.HentTiltakOgAktiviteterForBrukerPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.HentTiltakOgAktiviteterForBrukerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.HentTiltakOgAktiviteterForBrukerUgyldigInput;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbaktivitet.api.AktivitetController.ARENA_PREFIX;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.getDate;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.mergeDateTime;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class ArenaAktivitetConsumer {

    static final String DATOFILTER_PROPERTY_NAME = "arena.aktivitet.datofilter";

    private static final Logger LOG = getLogger(ArenaAktivitetConsumer.class);

    private static final String DATO_FORMAT = "yyyy-MM-dd";

    @Inject
    TiltakOgAktivitetV1 tiltakOgAktivitetV1;

    Date arenaAktivitetFilterDato;

    public ArenaAktivitetConsumer(@Value("${" + DATOFILTER_PROPERTY_NAME + ":}") String datoStreng) {
        this.arenaAktivitetFilterDato = parseDato(datoStreng);
    }

    static Date parseDato(String konfigurertDato) {
        try {
            return new SimpleDateFormat(DATO_FORMAT).parse(konfigurertDato);
        } catch (Exception e) {
            LOG.warn("Kunne ikke parse dato [{}] med datoformat [{}].", konfigurertDato, DATO_FORMAT);
            return null;
        }
    }

    public List<ArenaAktivitetDTO> hentArenaAktivieter(String personident) {

        val req = new HentTiltakOgAktiviteterForBrukerRequest();
        req.setPersonident(personident);
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
            return result.stream().filter(aktivitet -> etterFilterDato(aktivitet.getTilDato())).collect(toList());
        } catch (HentTiltakOgAktiviteterForBrukerPersonIkkeFunnet |
                HentTiltakOgAktiviteterForBrukerSikkerhetsbegrensning |
                HentTiltakOgAktiviteterForBrukerUgyldigInput e) {
            LOG.warn("Klarte ikke hente aktiviteter fra Arena.", e);
            return emptyList();
        }
    }

    private boolean etterFilterDato(Date tilDato) {
        return tilDato == null || arenaAktivitetFilterDato == null || arenaAktivitetFilterDato.before(tilDato);
    }

    private static final String VANLIG_AMO_NAVN = "Arbeidsmarkedsopplæring (AMO)";
    private static final String JOBBKLUBB_NAVN = "Jobbklubb";

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
                .setStatusSistEndret(getDate(tiltaksaktivitet.getStatusSistEndret()))
                .setOpprettetDato(getDate(tiltaksaktivitet.getStatusSistEndret()));


        val erVanligAmo = tiltaksaktivitet.getTiltaksnavn().trim()
                .equalsIgnoreCase(VANLIG_AMO_NAVN);

        val tittel = erVanligAmo ?
                "AMO-kurs: " + tiltaksaktivitet.getTiltakLokaltNavn() : tiltaksaktivitet.getTiltaksnavn();

        arenaAktivitetDTO.setTittel(tittel);

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

        Date startDato = motePlan.get(0).getStartDato();
        Date sluttDato = motePlan.get(motePlan.size() - 1).getSluttDato();
        AktivitetStatus status = gruppeaktivitet.getStatus().getValue().equals("AVBR") ?
                AktivitetStatus.AVBRUTT : mapTilAktivitetsStatus(startDato, sluttDato);
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

    private final static List<String> EGEN_FINANSERT_UTDANNELSE_NAVN = Arrays.asList(
            "ordinær utdanning for enslig forsørgere",
            "egenfinansiert utdanning",
            "egenfinansiert kurs");

    private ArenaAktivitetDTO mapTilAktivitet(Utdanningsaktivitet utdanningsaktivitet) {
        Date startDato = getDate(utdanningsaktivitet.getAktivitetPeriode().getFom());
        Date sluttDato = getDate(utdanningsaktivitet.getAktivitetPeriode().getTom());


        val tempStatus = mapTilAktivitetsStatus(startDato, sluttDato);

        val erEgenFinansiert = EGEN_FINANSERT_UTDANNELSE_NAVN.stream()
                .anyMatch(titel -> utdanningsaktivitet.getAktivitetstype().toLowerCase().startsWith(titel));

        val status = AktivitetStatus.AVBRUTT.equals(tempStatus) && erEgenFinansiert ?
                AktivitetStatus.FULLFORT : AktivitetStatus.AVBRUTT;

        return new ArenaAktivitetDTO()
                .setId(prefixArenaId(utdanningsaktivitet.getAktivitetId()))
                .setStatus(status)
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

    private AktivitetStatus mapTilAktivitetsStatus(Date startDato, Date sluttDato) {
        Date now = Calendar.getInstance().getTime();
        return now.before(startDato) ? AktivitetStatus.PLANLAGT : now.before(sluttDato) ?
                AktivitetStatus.GJENNOMFORES : AktivitetStatus.AVBRUTT;
    }

    private MoteplanDTO mapTilMoteplan(Moeteplan moeteplan) {
        return new MoteplanDTO()
                .setSted(moeteplan.getSted())
                .setStartDato(getDate(mergeDateTime(moeteplan.getStartDato(), moeteplan.getStartKlokkeslett())))
                .setSluttDato(getDate(mergeDateTime(moeteplan.getSluttDato(), moeteplan.getSluttKlokkeslett())));
    }

    private Date mapPeriodeToDate(Periode date, Function<Periode, XMLGregorianCalendar> periodeDate) {
        return Optional.ofNullable(date).map(periodeDate).map(DateUtils::getDate).orElse(null);
    }

    private enum ArenaStatus {
        AKTUELL(AktivitetStatus.PLANLAGT),
        AVSLAG(AktivitetStatus.AVBRUTT),
        DELAVB(AktivitetStatus.AVBRUTT),
        FULLF(AktivitetStatus.FULLFORT),
        GJENN(AktivitetStatus.GJENNOMFORES),
        GJENN_AVB(AktivitetStatus.AVBRUTT),
        GJENN_AVL(AktivitetStatus.AVBRUTT),
        IKKAKTUELL(AktivitetStatus.AVBRUTT),
        IKKEM(AktivitetStatus.AVBRUTT),
        INFOMOETE(AktivitetStatus.PLANLAGT),
        JATAKK(AktivitetStatus.PLANLAGT),
        NEITAKK(AktivitetStatus.AVBRUTT),
        TILBUD(AktivitetStatus.PLANLAGT),
        VENTELISTE(AktivitetStatus.PLANLAGT);

        ArenaStatus(AktivitetStatus status) {
            this.status = status;
        }

        private AktivitetStatus status;

        AktivitetStatus getStatus() {
            return status;
        }
    }

}
