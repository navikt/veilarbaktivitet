package no.nav.veilarbaktivitet.avtaltMedNav;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@Transactional
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/avtaltMedNav")
public class AvtaltMedNavController {

    private final AuthService authService;
    private final AvtaltMedNavService avtaltMedNavService;

    @PutMapping
    public AktivitetDTO markerSomAvtaltMedNav(@RequestBody AvtaltMedNav avtaltMedNav, @RequestParam long aktivitetId) {
        Forhaandsorientering forhaandsorientering = avtaltMedNav.getForhaandsorientering();

        if (forhaandsorientering == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "forhaandsorientering kan ikke være null");
        }

        if (forhaandsorientering.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "forhaandsorientering.type kan ikke være null");
        }

        AktivitetData aktivitet = avtaltMedNavService.hentAktivitet(aktivitetId);

        if (aktivitet == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aktiviteten eksisterer ikke");
        }

        authService.sjekkTilgangOgInternBruker(aktivitet.getAktorId(), aktivitet.getKontorsperreEnhetId());

        if (avtaltMedNav.getAktivitetVersjon() != aktivitet.getVersjon()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feil aktivitetversjon");
        }

        if (aktivitet.isAvtalt()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Aktiviteten er allerede avtalt med NAV");
        }

        Person innloggetBruker = authService.getLoggedInnUser().orElseThrow(RuntimeException::new);

        return avtaltMedNavService.markerSomAvtaltMedNav(aktivitetId, avtaltMedNav, innloggetBruker);

    }

    @PutMapping("/lest")
    public AktivitetDTO lest(@RequestBody LestDTO lestDTO) {

        if (lestDTO.aktivitetId == null || lestDTO.aktivitetVersion == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "aktivitetId og aktivitetVersion må vere satt");
        }

        AktivitetData aktivitet = avtaltMedNavService.hentAktivitet(lestDTO.aktivitetId);

        if (aktivitet == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aktiviteten eksisterer ikke");
        }

        authService.sjekkTilgangTilPerson(Person.aktorId(aktivitet.getAktorId()));

        if (aktivitet.getForhaandsorientering() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Fho eksister ikke");
        }

        if (aktivitet.getForhaandsorientering().getLest() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Allerede lest");
        }

        Person innloggetBruker = authService.getLoggedInnUser().orElseThrow(RuntimeException::new);
        return avtaltMedNavService.markerSomLest(aktivitet, innloggetBruker);
    }

}
