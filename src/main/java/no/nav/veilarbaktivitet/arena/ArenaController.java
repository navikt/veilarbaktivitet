package no.nav.veilarbaktivitet.arena;

import lombok.RequiredArgsConstructor;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.service.AuthService;
import no.nav.veilarbaktivitet.service.BrukerService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@Transactional
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/arena")
public class ArenaController {

    private final ArenaForhaandsorienteringDAO forhaandsorienteringDAO;
    private final HttpServletRequest requestProvider;
    private final BrukerService brukerService;
    private final AuthService authService;
    private final ArenaService arenaService;

    @PutMapping("/forhaandsorientering")
    public ArenaAktivitetDTO sendForhaandsorientering(@RequestBody Forhaandsorientering forhaandsorientering, @RequestParam String arenaaktivitetId) {

        Person person = getContextUserIdent();
        Person.AktorId aktorId = brukerService.getAktorIdForPerson(person).orElseThrow(RuntimeException::new);
        Person.Fnr fnr = brukerService.getFNR(person).orElseThrow(RuntimeException::new);
        authService.sjekkTilgangOgInternBruker(aktorId.get(), null);

        validerInput(forhaandsorientering);

        List<ArenaAktivitetDTO> arenaAktiviteter = arenaService.hentAktiviteter(fnr);

        ArenaAktivitetDTO arenaAktivitet = arenaAktiviteter
                .stream()
                .filter(arenaAktivitetDTO -> arenaAktivitetDTO.getId().equals(arenaaktivitetId))
                .findAny()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktiviteten eksisterer ikke"));

        if (arenaAktivitet.getForhaandsorientering() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Det er allerede lagt til forhåndsorientering på denne aktiviteten");
        }

        forhaandsorienteringDAO.insertForhaandsorientering(arenaaktivitetId, aktorId, forhaandsorientering);

        return arenaAktivitet.setForhaandsorientering(forhaandsorientering);
    }

    private Person getContextUserIdent() {
        if (BrukerService.erEksternBruker()) {
            return SubjectHandler.getIdent().map(Person::fnr).orElseThrow(RuntimeException::new);
        }

        Optional<Person> fnr = Optional.ofNullable(requestProvider.getParameter("fnr")).map(Person::fnr);
        Optional<Person> aktorId = Optional.ofNullable(requestProvider.getParameter("aktorId")).map(Person::aktorId);
        return fnr.orElseGet(() -> aktorId.orElseThrow(RuntimeException::new));
    }

    private void validerInput(Forhaandsorientering forhaandsorientering) {
        if (forhaandsorientering == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "forhaandsorientering kan ikke være null");
        }

        if (forhaandsorientering.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "forhaandsorientering.type kan ikke være null");
        }

        if (forhaandsorientering.getTekst() != null && forhaandsorientering.getTekst().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "forhaandsorientering.tekst kan ikke være null");
        }
    }
}
