package no.nav.veilarbaktivitet.arena;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.service.AuthService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.veilarbaktivitet.domain.AktivitetStatus.AVBRUTT;
import static no.nav.veilarbaktivitet.domain.AktivitetStatus.FULLFORT;

@Component
@RequiredArgsConstructor
public class ArenaService {
    private final ArenaAktivitetConsumer consumer;
    private final ArenaForhaandsorienteringDAO dao;
    private final AuthService authService;

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

    public ArenaAktivitetDTO lagreForhaandsorientering(String arenaaktivitetId, Person.Fnr fnr, Forhaandsorientering forhaandsorientering) throws ResponseStatusException {
        ArenaAktivitetDTO arenaAktivitetDTO = hentAktivitet(fnr, arenaaktivitetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktiviteten finnes ikke"));

        Person.AktorId aktorId = authService.getAktorIdForPersonBrukerService(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fant ikke aktorId"));

        try {
            dao.insertForhaandsorientering(arenaaktivitetId, aktorId, forhaandsorientering);
            return  arenaAktivitetDTO.setForhaandsorientering(forhaandsorientering);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Det er allerede sendt forhaandsorientering på aktiviteten");
        }
    }
}
