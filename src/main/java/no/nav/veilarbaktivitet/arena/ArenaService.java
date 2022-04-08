package no.nav.veilarbaktivitet.arena;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus.AVBRUTT;
import static no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus.FULLFORT;
import static no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService.FORHAANDSORIENTERING_DITT_NAV_TEKST;

@Slf4j
@Component
public class ArenaService {
    private final ArenaAktivitetConsumer consumer;
    private final ForhaandsorienteringDAO fhoDAO;
    private final AuthService authService;
    private final MeterRegistry meterRegistry;
    private final BrukernotifikasjonService brukernotifikasjonArenaAktivitetService;

    public static final String AVTALT_MED_NAV_COUNTER = "arena.avtalt.med.nav";
    public static final String AKTIVITET_TYPE_LABEL = "AktivitetType";
    public static final String FORHAANDSORIENTERING_TYPE_LABEL = "ForhaandsorienteringType";

    public ArenaService(ArenaAktivitetConsumer consumer, ForhaandsorienteringDAO fhoDAO, AuthService authService, MeterRegistry meterRegistry, BrukernotifikasjonService brukernotifikasjonArenaAktivitetService) {
        this.consumer = consumer;
        this.authService = authService;
        this.meterRegistry = meterRegistry;
        this.fhoDAO = fhoDAO;
        this.brukernotifikasjonArenaAktivitetService = brukernotifikasjonArenaAktivitetService;
        Counter.builder(AVTALT_MED_NAV_COUNTER)
                .description("Antall arena aktiviteter som er avtalt med NAV")
                .tags(AKTIVITET_TYPE_LABEL, "", FORHAANDSORIENTERING_TYPE_LABEL, "")
                .register(meterRegistry);
    }


    public List<ArenaAktivitetDTO> hentAktiviteter(Person.Fnr fnr) {
        List<ArenaAktivitetDTO> aktiviteterFraArena = consumer.hentArenaAktiviteter(fnr);

        Person.AktorId aktorId = authService.getAktorIdForPersonBrukerService(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fant ikke aktorId"));

        List<Forhaandsorientering> forhaandsorienteringData = fhoDAO.getAlleArenaFHO(aktorId);

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

    private Function<ArenaAktivitetDTO, ArenaAktivitetDTO> mergeMedForhaandsorientering(List<Forhaandsorientering> forhaandsorienteringData) {
        return arenaAktivitetDTO -> arenaAktivitetDTO.setForhaandsorientering(forhaandsorienteringData
                .stream()
                .filter(arenaForhaandsorienteringData -> arenaForhaandsorienteringData.getArenaAktivitetId().equals(arenaAktivitetDTO.getId()))
                .findAny()
                .map(AktivitetDTOMapper::mapForhaandsorientering)
                .orElse(null)
        );
    }

    @Transactional
    public ArenaAktivitetDTO opprettFHO(String arenaaktivitetId, Person.Fnr fnr, ForhaandsorienteringDTO forhaandsorientering, String opprettetAv) throws ResponseStatusException {
        ArenaAktivitetDTO arenaAktivitetDTO = hentAktivitet(fnr, arenaaktivitetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktiviteten finnes ikke"));

        Person.AktorId aktorId = authService.getAktorIdForPersonBrukerService(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fant ikke aktorId"));

        var fho = fhoDAO.getFhoForArenaAktivitet(arenaaktivitetId);

        if(fho != null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Det er allerede sendt forhaandsorientering på aktiviteten");
        }

        if (!brukernotifikasjonArenaAktivitetService.kanVarsles(aktorId)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Bruker kan ikke varsles");
        }

        brukernotifikasjonArenaAktivitetService.opprettVarselPaaArenaAktivitet(arenaaktivitetId, fnr, FORHAANDSORIENTERING_DITT_NAV_TEKST, VarselType.FORHAANDSORENTERING);

        var nyForhaandsorientering = fhoDAO.insertForArenaAktivitet(forhaandsorientering, arenaaktivitetId, aktorId, opprettetAv, new Date());
        meterRegistry.counter(AVTALT_MED_NAV_COUNTER, FORHAANDSORIENTERING_TYPE_LABEL, forhaandsorientering.getType().name(), AKTIVITET_TYPE_LABEL, arenaAktivitetDTO.getType().name()).increment();
        return arenaAktivitetDTO.setForhaandsorientering(nyForhaandsorientering.toDTO());
    }

    @Transactional
    public ArenaAktivitetDTO markerSomLest(Person.Fnr fnr, String aktivitetId) {
        var fho = fhoDAO.getFhoForArenaAktivitet(aktivitetId);

        if(fho == null) {
            log.warn("Kan ikke markere forhåndsorientering som lest. Fant ikke forhåndsorientering for aktivitet med id:" + aktivitetId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Forhåndsorientering finnes ikke");
        }

        if(fho.getLestDato() != null) {
            log.warn("Kan ikke markere forhåndsorientering som lest. Forhåndsorienteringen for aktivitet med id:" + aktivitetId + " er allerede lest");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Forhåndsorienteringen er allerede lest");
        }

        brukernotifikasjonArenaAktivitetService.setDone(aktivitetId, VarselType.FORHAANDSORENTERING);

        fhoDAO.markerSomLest(fho.getId(), new Date(), null);

        return hentAktivitet(fnr, aktivitetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kunne ikke hente aktiviteten"));

    }
}
