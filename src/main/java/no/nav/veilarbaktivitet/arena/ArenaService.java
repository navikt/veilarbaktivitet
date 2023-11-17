package no.nav.veilarbaktivitet.arena;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO;
import no.nav.veilarbaktivitet.arena.model.AktiviteterDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDAO;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus.AVBRUTT;
import static no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus.FULLFORT;
import static no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService.FORHAANDSORIENTERING_DITT_NAV_TEKST;

@Slf4j
@Component
public class ArenaService {
    private final ForhaandsorienteringDAO fhoDAO;
    private final IdMappingDAO idMappingDAO;
    private final MeterRegistry meterRegistry;
    private final BrukernotifikasjonService brukernotifikasjonArenaAktivitetService;
    private final VeilarbarenaClient veilarbarenaClient;
    private final PersonService personService;

    public static final String AVTALT_MED_NAV_COUNTER = "arena.avtalt.med.nav";
    public static final String AKTIVITET_TYPE_LABEL = "AktivitetType";
    public static final String FORHAANDSORIENTERING_TYPE_LABEL = "ForhaandsorienteringType";

    public ArenaService(
            ForhaandsorienteringDAO fhoDAO,
            MeterRegistry meterRegistry,
            BrukernotifikasjonService brukernotifikasjonArenaAktivitetService,
            VeilarbarenaClient veilarbarenaClient,
            IdMappingDAO idMappingDAO,
            PersonService personService
    ) {
        this.meterRegistry = meterRegistry;
        this.fhoDAO = fhoDAO;
        this.brukernotifikasjonArenaAktivitetService = brukernotifikasjonArenaAktivitetService;
        this.veilarbarenaClient = veilarbarenaClient;
        this.idMappingDAO = idMappingDAO;
        this.personService = personService;
        Counter.builder(AVTALT_MED_NAV_COUNTER)
                .description("Antall arena aktiviteter som er avtalt med NAV")
                .tags(AKTIVITET_TYPE_LABEL, "", FORHAANDSORIENTERING_TYPE_LABEL, "")
                .register(meterRegistry);
    }

    public List<ArenaAktivitetDTO> hentAktiviteter(Person.Fnr fnr) {
        Optional<AktiviteterDTO> aktiviteterFraArena = veilarbarenaClient.hentAktiviteter(fnr);

        if (aktiviteterFraArena.isEmpty()) return List.of();

        List<ArenaAktivitetDTO> aktiviteter = VeilarbarenaMapper.map(aktiviteterFraArena.get());

        Person.AktorId aktorId = personService.getAktorIdForPersonBruker(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fant ikke aktorId"));

        List<Forhaandsorientering> forhaandsorienteringData = fhoDAO.getAlleArenaFHO(aktorId);

        return aktiviteter
                .stream()
                .map(mergeMedForhaandsorientering(forhaandsorienteringData))
                .toList();
    }

    public boolean harAktiveTiltak(Person.Fnr ident) {
        return hentAktiviteter(ident)
                .stream()
                .map(ArenaAktivitetDTO::getStatus)
                .anyMatch(status -> status != AVBRUTT && status != FULLFORT);
    }

    public Optional<ArenaAktivitetDTO> hentAktivitet(Person.Fnr ident, ArenaId aktivitetId) {
        return hentAktiviteter(ident).stream()
                .filter(arenaAktivitetDTO -> aktivitetId.id().equals(arenaAktivitetDTO.getId()))
                .findAny();
    }

    public Function<ArenaAktivitetDTO, ArenaAktivitetDTO> mergeMedForhaandsorientering(List<Forhaandsorientering> forhaandsorienteringData) {
        return arenaAktivitetDTO -> arenaAktivitetDTO.setForhaandsorientering(forhaandsorienteringData
                .stream()
                .filter(arenaForhaandsorienteringData -> arenaForhaandsorienteringData.getArenaAktivitetId().equals(arenaAktivitetDTO.getId()))
                .max(Comparator.comparing(Forhaandsorientering::getOpprettetDato))
                .map(AktivitetDTOMapper::mapForhaandsorientering)
                .orElse(null)
        );
    }

    @Transactional
    public ArenaAktivitetDTO opprettFHO(ArenaId arenaaktivitetId, Person.Fnr fnr, ForhaandsorienteringDTO forhaandsorientering, String opprettetAv) throws ResponseStatusException {
        ArenaAktivitetDTO arenaAktivitetDTO = hentAktivitet(fnr, arenaaktivitetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktiviteten finnes ikke"));

        Person.AktorId aktorId = personService.getAktorIdForPersonBruker(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fant ikke aktorId"));

        var fho = fhoDAO.getFhoForArenaAktivitet(arenaaktivitetId);

        if(fho != null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Det er allerede sendt forhaandsorientering på aktiviteten");
        }

        if (!brukernotifikasjonArenaAktivitetService.kanVarsles(aktorId)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Bruker kan ikke varsles");
        }

        var aktivitetId = idMappingDAO.getLatestAktivitetsId(arenaaktivitetId);
        brukernotifikasjonArenaAktivitetService.opprettVarselPaaArenaAktivitet(arenaaktivitetId, aktivitetId, fnr, FORHAANDSORIENTERING_DITT_NAV_TEKST, VarselType.FORHAANDSORENTERING);

        var nyForhaandsorientering = fhoDAO.insertForArenaAktivitet(forhaandsorientering, arenaaktivitetId, aktorId, opprettetAv, new Date(), aktivitetId);
        meterRegistry.counter(AVTALT_MED_NAV_COUNTER, FORHAANDSORIENTERING_TYPE_LABEL, forhaandsorientering.getType().name(), AKTIVITET_TYPE_LABEL, arenaAktivitetDTO.getType().name()).increment();
        return arenaAktivitetDTO.setForhaandsorientering(nyForhaandsorientering.toDTO());
    }

    @Transactional
    public ArenaAktivitetDTO markerSomLest(Person.Fnr fnr, ArenaId aktivitetId) {
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
