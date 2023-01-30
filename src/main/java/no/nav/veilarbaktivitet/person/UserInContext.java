package no.nav.veilarbaktivitet.person;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NorskIdent;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserInContext {
    private final HttpServletRequest requestProvider;
    private final IAuthService authService;
    private final PersonService personService;

    public Optional<Person.Fnr> getFnr() {
        var user = authService.getLoggedInnUser();
        if (user instanceof Fnr || user instanceof NorskIdent) {
            return Optional.of(Person.fnr(user.get()));
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
