package no.nav.veilarbaktivitet.arena;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.person.AuthService;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.UserInContext;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Transactional
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/arena")
public class ArenaController {
    private final UserInContext userInContext;
    private final AuthService authService;
    private final ArenaService arenaService;

    @PutMapping("/forhaandsorientering")
    public ArenaAktivitetDTO opprettFHO(@RequestBody ForhaandsorienteringDTO forhaandsorientering, @RequestParam String arenaaktivitetId) {
        if (!authService.erInternBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Må være internbruker");
        }

        getInputFeilmelding(forhaandsorientering, arenaaktivitetId)
                .ifPresent( feilmelding -> {throw new ResponseStatusException(HttpStatus.BAD_REQUEST, feilmelding);});

        Person.Fnr fnr = userInContext.getFnr()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finner ikke fnr"));
        authService.sjekkTilgangTilPerson(fnr);

        String ident = authService.getInnloggetBrukerIdent()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "finner ikke veileder ident"));

        return arenaService.opprettFHO(arenaaktivitetId, fnr, forhaandsorientering, ident);
    }

    @GetMapping("/tiltak")
    public List<ArenaAktivitetDTO> hentArenaAktiviteter() {
        Person.Fnr fnr = userInContext.getFnr().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Må være på en bruker"));
        authService.sjekkTilgangTilPerson(fnr);

        return arenaService.hentAktiviteter(fnr);
    }


    @GetMapping("/harTiltak")
    boolean hentHarTiltak() {
        Person.Fnr fnr = userInContext.getFnr().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Må være på en bruker"));
        authService.sjekkTilgangTilPerson(fnr);

        return arenaService.harAktiveTiltak(fnr);
    }

    @PutMapping("/forhaandsorientering/lest")
    ArenaAktivitetDTO lest(@RequestParam String aktivitetId) {
        Person.Fnr fnr = userInContext.getFnr().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Må være på en bruker"));
        authService.sjekkTilgangTilPerson(fnr);

        return arenaService.markerSomLest(fnr, aktivitetId);
    }

    private Optional<String> getInputFeilmelding(ForhaandsorienteringDTO forhaandsorientering, String arenaaktivitetId) {
        if(arenaaktivitetId == null || arenaaktivitetId.isBlank()) {
            return Optional.of("arenaaktivitetId kan ikke være null eller tom");
        }

        if (forhaandsorientering == null) {
            return Optional.of("forhaandsorientering kan ikke være null");
        }

        if (forhaandsorientering.getType() == null) {
            return Optional.of("forhaandsorientering.type kan ikke være null");
        }

        if (forhaandsorientering.getTekst() == null || forhaandsorientering.getTekst().isEmpty()) {
            return Optional.of("forhaandsorientering.tekst kan ikke være null eller tom");
        }
        return Optional.empty();
    }
}
