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

import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.getDate;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.mergeDateTime;
import static org.slf4j.LoggerFactory.getLogger;

public class ArenaAktivitetConsumer {

    private static final Logger LOG = getLogger(ArenaAktivitetConsumer.class);

    @Inject
    private TiltakOgAktivitetV1 tiltakOgAktivitetV1;

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
            return result;
        } catch (HentTiltakOgAktiviteterForBrukerPersonIkkeFunnet |
                HentTiltakOgAktiviteterForBrukerSikkerhetsbegrensning |
                HentTiltakOgAktiviteterForBrukerUgyldigInput e) {
            LOG.warn("Klarte ikke hente aktiviteter fra Arena.", e);
            return emptyList();
        }
    }

    private ArenaAktivitetDTO mapTilAktivitet(Tiltaksaktivitet tiltaksaktivitet) {
        String titttel = tiltaksaktivitet.getTiltaksnavn().startsWith("Arbeidsmarkedsopplæring") ?
                "AMO-kurs: " + tiltaksaktivitet.getTiltakLokaltNavn() : tiltaksaktivitet.getTiltaksnavn();

        return new ArenaAktivitetDTO()
                .setId(randomArenaId())
                .setStatus(EnumUtils.valueOf(ArenaStatus.class, tiltaksaktivitet.getDeltakerStatus().getValue()).getStatus())
                .setType(ArenaAktivitetTypeDTO.TILTAKSAKTIVITET)
                .setTittel(titttel)
                .setBeskrivelse(tiltaksaktivitet.getTiltakLokaltNavn())
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
                .setOpprettetDato(getDate(tiltaksaktivitet.getStatusSistEndret()))
                .setEtikett(EnumUtils.valueOf(ArenaStatusDTO.class, tiltaksaktivitet.getDeltakerStatus().getValue()));
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
        return new ArenaAktivitetDTO()
                .setId(randomArenaId())
                .setStatus(mapTilAktivitetsStatus(startDato, sluttDato))
                .setTittel(StringUtils.capitalize(gruppeaktivitet.getAktivitetstype()))
                .setType(ArenaAktivitetTypeDTO.GRUPPEAKTIVITET)
                .setBeskrivelse(gruppeaktivitet.getBeskrivelse())
                .setFraDato(startDato)
                .setTilDato(sluttDato)
                .setAvtalt(true)
                .setMoeteplanListe(motePlan);
    }

    private ArenaAktivitetDTO mapTilAktivitet(Utdanningsaktivitet utdanningsaktivitet) {
        Date startDato = getDate(utdanningsaktivitet.getAktivitetPeriode().getFom());
        Date sluttDato = getDate(utdanningsaktivitet.getAktivitetPeriode().getTom());
        return new ArenaAktivitetDTO()
                .setId(randomArenaId())
                .setStatus(mapTilAktivitetsStatus(startDato, sluttDato))
                .setType(ArenaAktivitetTypeDTO.UTDANNINGSAKTIVITET)
                .setTittel(utdanningsaktivitet.getAktivitetstype())
                .setBeskrivelse(utdanningsaktivitet.getBeskrivelse())
                .setFraDato(startDato)
                .setTilDato(sluttDato)
                .setAvtalt(true);
    }

    private AktivitetStatus mapTilAktivitetsStatus(Date startDato, Date sluttDato) {
        Date now = Calendar.getInstance().getTime();
        return now.before(startDato) ? AktivitetStatus.PLANLAGT : now.before(sluttDato) ?
                AktivitetStatus.GJENNOMFORT : AktivitetStatus.AVBRUTT;
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

    private String randomArenaId() {
        // TODO: Fjern denne når PK-43443 er løst.
        return "arena" + String.valueOf(new Random().nextInt(100000));
    }

    private enum ArenaStatus {
        AKTUELL(AktivitetStatus.PLANLAGT),
        AVSLAG(AktivitetStatus.AVBRUTT),
        DELAVB(AktivitetStatus.AVBRUTT),
        FULLF(AktivitetStatus.FULLFORT),
        GJENN(AktivitetStatus.GJENNOMFORT),
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
