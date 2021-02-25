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

import static no.nav.veilarbaktivitet.domain.AktivitetStatus.AVBRUTT;
import static no.nav.veilarbaktivitet.domain.AktivitetStatus.FULLFORT;

@Transactional
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/arena")
public class ArenaController {
    private final ArenaForhaandsorienteringDAO forhaandsorienteringDAO;
    private final UserInContext userInContext;
    private final AuthService authService;
    private final ArenaService arenaService;

    @PutMapping("/forhaandsorientering")
    public ArenaAktivitetDTO sendForhaandsorientering(@RequestBody Forhaandsorientering forhaandsorientering, @RequestParam String arenaaktivitetId) {
        Person.AktorId aktorId = userInContext.getAktorId().orElseThrow(RuntimeException::new);
        authService.sjekkTilgangOgInternBruker(aktorId.get(), null);

        getInputFeilmelding(forhaandsorientering)
                .ifPresent( feilmelding -> {throw new ResponseStatusException(HttpStatus.BAD_REQUEST, feilmelding);});

        Person.Fnr fnr = userInContext.getFnr().orElseThrow(RuntimeException::new);
        ArenaAktivitetDTO arenaAktivitet = arenaService.hentAktivitet(fnr, arenaaktivitetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktiviteten eksisterer ikke"));

        if (arenaAktivitet.getForhaandsorientering() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Det er allerede lagt til forhåndsorientering på denne aktiviteten");
        }

        forhaandsorienteringDAO.insertForhaandsorientering(arenaaktivitetId, aktorId, forhaandsorientering);

        return arenaAktivitet.setForhaandsorientering(forhaandsorientering);
    }

    @GetMapping("/tiltak")
    public List<ArenaAktivitetDTO> hentArenaAktiviteter() {

        Person.Fnr fnr = userInContext.getFnr().orElseThrow(RuntimeException::new);
        authService.sjekkTilgangTilPerson(fnr);

        return arenaService.hentAktiviteter(fnr);
    }

    @GetMapping("/harTiltak")
    public boolean hentHarTiltak() {
        Person.Fnr fnr = userInContext.getFnr().orElseThrow(RuntimeException::new);
        authService.sjekkTilgangTilPerson(fnr);

        return arenaService.hentAktiviteter(fnr)
                .stream()
                .map(ArenaAktivitetDTO::getStatus)
                .anyMatch(status -> status != AVBRUTT && status != FULLFORT);
    }


    private Optional<String> getInputFeilmelding(Forhaandsorientering forhaandsorientering) {
        if (forhaandsorientering == null) {
            return Optional.of("forhaandsorientering kan ikke være null");
        }

        if (forhaandsorientering.getType() == null) {
            return Optional.of("forhaandsorientering.type kan ikke være null");
        }

        if (forhaandsorientering.getTekst() != null && forhaandsorientering.getTekst().isEmpty()) {
            return Optional.of("forhaandsorientering.tekst kan ikke være null");
        }
        return Optional.empty();
    }
}
