package no.nav.veilarbaktivitet.person;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarbaktivitet.util.AuthUtils;
import org.apache.cxf.common.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.ParseException;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AuthContextHolder authContextHolder;

    private final Pep veilarbPep;

    private final PersonService personService;

    public void sjekkEksternBrukerHarTilgang(Person ident) {
        var loggedInUserFnr = getInnloggetBrukerIdent();
        if (!loggedInUserFnr.map(fnr -> fnr.equals(ident.get())).orElse(false)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "ekstern bruker har ikke tilgang til andre brukere enn seg selv"
            );
        }
        if (!eksternBrukerHasNiva4()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "ekstern bruker har ikke innloggingsnivå 4"
            );
        }
    }

    private boolean eksternBrukerHasNiva4() {
        return authContextHolder.getIdTokenClaims()
                .map(jwtClaimsSet -> {
                    try {
                        return Objects.equals(jwtClaimsSet.getStringClaim("acr"), "Level4");
                    } catch (ParseException e) {
                        return false;
                    }
                }).orElse(false);
    }

    private Person.Fnr getFnrForEksternBruker(Person ident) {
        if (ident instanceof Person.Fnr) return (Person.Fnr) ident;
        if (ident instanceof Person.AktorId) return personService.getFnrForAktorId(Person.aktorId(ident.get()));
        throw new IllegalArgumentException("Kan ikke hente fnr for NAV-ansatte");
    }

    public void sjekkTilgangTilPerson(Person ident) {
        if (erEksternBruker()) {
            sjekkEksternBrukerHarTilgang(getFnrForEksternBruker(ident));
            return;
        }

        String aktorId = personService
                .getAktorIdForPersonBruker(ident)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN)).get();

        String innloggetBrukerToken = authContextHolder
                .getIdTokenString()
                .orElseThrow(() -> new IllegalStateException("Fant ikke token til innlogget bruker"));

        if (AuthUtils.erSystemkallFraAzureAd(authContextHolder)) {
            return;
        }

        if (!veilarbPep.harTilgangTilPerson(innloggetBrukerToken, ActionId.READ, AktorId.of(aktorId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public void sjekkTilgangTilPerson(AktorId aktorId) {
        String innloggetBrukerToken = authContextHolder
                .getIdTokenString()
                .orElseThrow(() -> new IllegalStateException("Fant ikke token til innlogget bruker"));

        if (!veilarbPep.harTilgangTilPerson(innloggetBrukerToken, ActionId.READ, aktorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public void sjekkTilgangOgInternBruker(String aktorid, String enhet) {
        if (!authContextHolder.erInternBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        sjekkTilgang(aktorid, enhet);
    }

    public void sjekkTilgang(String aktorid, String enhet) {
        sjekkTilgangTilPerson(Person.aktorId(aktorid));

        if (!sjekKvpTilgang(enhet)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public Optional<Person.AktorId> getAktorIdForPersonBrukerService(Person person) {
        return personService.getAktorIdForPersonBruker(person);
    }

    public boolean sjekKvpTilgang(String enhet) {
        if (StringUtils.isEmpty(enhet)) {
            return true;
        }

        if (authContextHolder.erEksternBruker()) {
            return true;
        }

        return veilarbPep.harVeilederTilgangTilEnhet(getInnloggetVeilederIdent(), EnhetId.of(enhet));
    }

    public void sjekkVeilederHarSkriveTilgangTilPerson(String aktorId) {
        boolean harTilgang = veilarbPep.harVeilederTilgangTilPerson(getInnloggetVeilederIdent(), ActionId.WRITE, AktorId.of(aktorId));
        if (!harTilgang) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public NavIdent getInnloggetVeilederIdent() {
        if (authContextHolder.requireRole() != UserRole.INTERN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bruker er ikke veileder");
        }

        return authContextHolder
                .getNavIdent()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fant ikke ident for innlogget veileder"));
    }

    public Optional<Person> getLoggedInnUser() {
        return authContextHolder
                .getRole()
                .flatMap((role) -> {
                    if (UserRole.EKSTERN.equals(role)) {
                        return getAktorIdForEksternBruker();
                    }
                    if (UserRole.INTERN.equals(role)) {
                        return authContextHolder.getNavIdent().map(ident -> Person.navIdent(ident.get()));
                    }
                    if (UserRole.SYSTEM.equals(role)) {
                        return authContextHolder.getSubject().map(Person::navIdent);
                    }

                    return Optional.empty();
                });
    }

    public Optional<String> getInnloggetBrukerIdent() {
        return authContextHolder.getUid();
    }

    public boolean erEksternBruker() {
        return authContextHolder.erEksternBruker();
    }

    public boolean erInternBruker() {
        return authContextHolder.erInternBruker();
    }

    public boolean erSystemBruker() {
        return authContextHolder.erSystemBruker();
    }

    private Optional<Person.AktorId> getAktorIdForEksternBruker() {
        return authContextHolder.erEksternBruker()
                ? authContextHolder.getUid().flatMap(sub -> personService.getAktorIdForPersonBruker(Person.fnr(sub)))
                : Optional.empty();
    }
}
