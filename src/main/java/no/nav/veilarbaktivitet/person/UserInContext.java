package no.nav.veilarbaktivitet.person;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NorskIdent;
import no.nav.poao.dab.spring_auth.IAuthService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserInContext {
    private final HttpServletRequest requestProvider;
    private final IAuthService authService;
    private final PersonService personService;
    private final AktorOppslagClient aktorOppslagClient;

    public Optional<Person.Fnr> getFnr() {
        var user = authService.getLoggedInnUser();
        if (user instanceof Fnr || user instanceof NorskIdent) {
            return Optional.of(Person.fnr(user.get()));
        }

        Optional<Person.Fnr> fnrFromRequestAttribute = Optional
                .ofNullable(requestProvider.getAttribute("fnr"))
                .map((it) -> (Fnr) it )
                .map((it) -> Person.fnr(it.get()));

        Optional<Person.Fnr> fnr = Optional
                .ofNullable(requestProvider.getParameter("fnr"))
                .map(Person::fnr);

        Optional<Person> aktorId = Optional
                .ofNullable(requestProvider.getParameter("aktorId"))
                .map(Person::aktorId);

        return fnrFromRequestAttribute.or(() -> fnr.or(() -> aktorId.flatMap(personService::getFnrForPersonbruker)));
    }

    public Person.AktorId getAktorId() {
        var user = authService.getLoggedInnUser();
        if (user instanceof Fnr || user instanceof NorskIdent) {
            return Person.aktorId(aktorOppslagClient.hentAktorId(Fnr.of(user.get())).get());
        }
        Optional<Person.AktorId> aktorIdFromRequestAttribute = Optional
                .ofNullable(requestProvider.getAttribute("fnr"))
                .map((it) -> (Fnr) it )
                .map(aktorOppslagClient::hentAktorId)
                .map(aktorId -> Person.aktorId(aktorId.get()));
        Optional<Person.AktorId> aktorIdFromFnr = Optional
                .ofNullable(requestProvider.getParameter("fnr"))
                .map(fnr -> aktorOppslagClient.hentAktorId(Fnr.of(fnr)))
                .map(aktorId -> Person.aktorId(aktorId.get()));
        Optional<Person.AktorId> aktorId = Optional
                .ofNullable(requestProvider.getParameter("aktorId"))
                .map(Person::aktorId);
        return aktorIdFromRequestAttribute
                .or(() -> aktorId.or(() -> aktorIdFromFnr))
                .orElseThrow();
    }

    public EksternBruker getEksternBruker() {
        Person.Fnr fnr = getFnr().orElseThrow();
        Person.AktorId aktorId = getAktorId();
        return new EksternBruker(fnr, aktorId);
    }
}
