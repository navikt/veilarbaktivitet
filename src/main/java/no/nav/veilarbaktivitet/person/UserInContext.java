package no.nav.veilarbaktivitet.person;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserInContext {
    private final HttpServletRequest requestProvider;
    private final AuthService authService;
    private final PersonService personService;

    public Optional<Person.Fnr> getFnr() {
        if (authService.erEksternBruker()) {
            return authService.getInnloggetBrukerIdent().map(Person::fnr);
        }

        Optional<Person> fnr = Optional
                .ofNullable(requestProvider.getParameter("fnr"))
                .map(Person::fnr);

        Optional<Person> aktorId = Optional
                .ofNullable(requestProvider.getParameter("aktorId"))
                .map(Person::aktorId);

        return fnr.or(() -> aktorId)
                .flatMap(personService::getFnrForPersonbruker);
    }
}
