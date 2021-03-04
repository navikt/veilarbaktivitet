package no.nav.veilarbaktivitet.arena;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.service.AuthService;
import no.nav.veilarbaktivitet.service.UserInContext;
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
    ArenaAktivitetDTO sendForhaandsorientering(@RequestBody Forhaandsorientering forhaandsorientering, @RequestParam String arenaaktivitetId) {

        Person person = userInContext.getUserIdent().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Må være på en bruker"));
        Person.AktorId aktorId = userInContext.getAktorId(person).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Må være på en bruker"));

        authService.sjekkTilgangOgInternBruker(aktorId.get(), null);

        getInputFeilmelding(forhaandsorientering, arenaaktivitetId)
                .ifPresent( feilmelding -> {throw new ResponseStatusException(HttpStatus.BAD_REQUEST, feilmelding);});

        Person.Fnr fnr = userInContext.getFnr(person).orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Finner ikke fnr for bruker"));

        try {
            return arenaService.lagreForhaandsorientering(arenaaktivitetId, aktorId, fnr, forhaandsorientering);
        } catch (BadRequestException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/tiltak")
    List<ArenaAktivitetDTO> hentArenaAktiviteter() {

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


    private Optional<String> getInputFeilmelding(Forhaandsorientering forhaandsorientering, String arenaaktivitetId) {
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
