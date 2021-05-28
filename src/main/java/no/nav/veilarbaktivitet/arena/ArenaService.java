package no.nav.veilarbaktivitet.arena;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.service.AuthService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.veilarbaktivitet.domain.AktivitetStatus.AVBRUTT;
import static no.nav.veilarbaktivitet.domain.AktivitetStatus.FULLFORT;

@Slf4j
@Component
public class ArenaService {
    private final ArenaAktivitetConsumer consumer;
    private final ArenaForhaandsorienteringDAO dao;
    private final AuthService authService;
    private final MeterRegistry meterRegistry;

    public static final String AVTALT_MED_NAV_COUNTER = "arena.avtalt.med.nav";
    public static final String AKTIVITET_TYPE_LABEL = "AktivitetType";
    public static final String FORHAANDSORIENTERING_TYPE_LABEL = "ForhaandsorienteringType";

    @java.beans.ConstructorProperties({"consumer", "dao", "authService", "meterRegistry"})
    public ArenaService(ArenaAktivitetConsumer consumer, ArenaForhaandsorienteringDAO dao, AuthService authService, MeterRegistry meterRegistry) {
        this.consumer = consumer;
        this.dao = dao;
        this.authService = authService;
        this.meterRegistry = meterRegistry;
        Counter.builder(AVTALT_MED_NAV_COUNTER)
                .description("Antall arena aktiviteter som er avtalt med NAV")
                .tags(AKTIVITET_TYPE_LABEL, "", FORHAANDSORIENTERING_TYPE_LABEL, "")
                .register(meterRegistry);
    }


    public List<ArenaAktivitetDTO> hentAktiviteter(Person.Fnr fnr) {
        List<ArenaAktivitetDTO> aktiviteterFraArena = consumer.hentArenaAktiviteter(fnr);

        Person.AktorId aktorId = authService.getAktorIdForPersonBrukerService(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fant ikke aktorId"));

        List<ArenaForhaandsorienteringData> forhaandsorienteringData = dao.hentForhaandsorienteringer(aktorId);

        return aktiviteterFraArena
                .stream()
                .map(mergeMedForhaandsorientering(forhaandsorienteringData))
                .collect(Collectors.toList());
    }

    public boolean harAktiveTiltak(Person.Fnr ident) {
        return consumer.hentArenaAktiviteter(ident)
                .stream()
                .map(ArenaAktivitetDTO::getStatus)
                .anyMatch(status -> status != AVBRUTT && status != FULLFORT);
    }

    public Optional<ArenaAktivitetDTO> hentAktivitet(Person.Fnr ident, String aktivitetId) {
        return hentAktiviteter(ident).stream().filter(arenaAktivitetDTO -> aktivitetId.equals(arenaAktivitetDTO.getId())).findAny();
    }

    private Function<ArenaAktivitetDTO, ArenaAktivitetDTO> mergeMedForhaandsorientering(List<ArenaForhaandsorienteringData> forhaandsorienteringData) {
        return arenaAktivitetDTO -> arenaAktivitetDTO.setForhaandsorientering(forhaandsorienteringData
                .stream()
                .filter(arenaForhaandsorienteringData -> arenaForhaandsorienteringData.getArenaktivitetId().equals(arenaAktivitetDTO.getId()))
                .findAny()
                .map(ArenaForhaandsorienteringData::getForhaandsorientering)
                .orElse(null)
        );
    }

    public ArenaAktivitetDTO lagreForhaandsorientering(String arenaaktivitetId, Person.Fnr fnr, Forhaandsorientering forhaandsorientering, String opprettetAv) throws ResponseStatusException {
        ArenaAktivitetDTO arenaAktivitetDTO = hentAktivitet(fnr, arenaaktivitetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktiviteten finnes ikke"));

        Person.AktorId aktorId = authService.getAktorIdForPersonBrukerService(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fant ikke aktorId"));

        try {
            dao.insertForhaandsorientering(arenaaktivitetId, aktorId, forhaandsorientering, opprettetAv);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Det er allerede sendt forhaandsorientering på aktiviteten");
        }

        meterRegistry.counter(AVTALT_MED_NAV_COUNTER, FORHAANDSORIENTERING_TYPE_LABEL, forhaandsorientering.getType().name(), AKTIVITET_TYPE_LABEL, arenaAktivitetDTO.getType().name()).increment();
        return  arenaAktivitetDTO.setForhaandsorientering(forhaandsorientering);
    }

    @Transactional
    public ArenaAktivitetDTO markerSomLest(Person.Fnr fnr, String aktivitetId) {
        Person.AktorId aktorId = authService.getAktorIdForPersonBrukerService(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fant ikke aktorId"));

        boolean oppdatert = dao.markerSomLest(aktorId, aktivitetId);
        if(!oppdatert) {
            log.warn("kunne ikke markere forhondsorentering på arena aktivitet " + aktivitetId + " som lest");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kan ikke markere aktiviteten som lest");
        }

        return hentAktivitet(fnr, aktivitetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kunne ikke hente aktiviteten"));

    }
}
