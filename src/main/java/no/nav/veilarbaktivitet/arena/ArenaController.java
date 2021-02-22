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

    @PutMapping("/forhaandsorientering")
    public ArenaAktivitetDTO sendForhaandsorientering(@RequestBody ForhaandsorienteringBody forhaandsorienteringData, @RequestParam String arenaaktivitetId) {

        Person person = getContextUserIdent();
        Person.AktorId aktorId = brukerService.getAktorIdForPerson(person).orElseThrow(RuntimeException::new);
        authService.sjekkTilgangOgInternBruker(aktorId.get(), null);

        Forhaandsorientering forhaandsorientering = forhaandsorienteringData.getForhaandsorientering();

        validerInput(forhaandsorientering);

        //TODO: Sjekke at FHO ikke allerede er sendt

        //TODO: Sjekke mot arena for å sjekke om aktiviteten finnes?
        //ArenaAktivitetDTO aktivitet = hentArenaaktivitet(arenaaktivitetId);

        if (aktivitet == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aktiviteten eksisterer ikke");
        }

        if (forhaandsorientering.getTekst() != null && forhaandsorientering.getTekst().isEmpty()) {
            forhaandsorientering.setTekst(null);
        }

        forhaandsorienteringDAO.insertForhaandsorientering(arenaaktivitetId, aktorId, forhaandsorientering);

        //TODO: returnere arenaaktivitet med FHO
        return ArenaAktivitetDTO.mapTilAktivitetDTO(aktivitetDAO.hentAktivitet(arenaaktivitetId));

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
    }
}
