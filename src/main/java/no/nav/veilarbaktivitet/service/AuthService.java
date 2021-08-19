package no.nav.veilarbaktivitet.service;

import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.UserRole;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.*;
import no.nav.veilarbaktivitet.domain.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {

    private final AuthContextHolder authContextHolder;

    private final AktorOppslagClient aktorOppslagClient;

    private final Pep veilarbPep;

    @Autowired
    public AuthService(AuthContextHolder authContextHolder, AktorOppslagClient aktorOppslagClient, Pep veilarbPep) {
        this.authContextHolder = authContextHolder;
        this.aktorOppslagClient = aktorOppslagClient;
        this.veilarbPep = veilarbPep;
    }


    public void sjekkTilgangTilPerson(Person ident) {
        String aktorId = getAktorIdForPerson(ident);
        String innloggetBrukerToken = authContextHolder
                .getIdTokenString()
                .orElseThrow(() -> new IllegalStateException("Fant ikke token til innlogget bruker"));

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
        if(!authContextHolder.erInternBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        sjekkTilgang(aktorid, enhet);
    }

    public void sjekkTilgang(String aktorid, String enhet) {
        sjekkTilgangTilPerson(Person.aktorId(aktorid));

        if(!sjekKvpTilgang(enhet)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private String getAktorIdForPerson(Person person){
        if (person instanceof Person.AktorId) {
            return person.get();
        } else if (person instanceof  Person.Fnr){
            return aktorOppslagClient.hentAktorId(Fnr.of(person.get())).get();
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public Optional<Person.AktorId> getAktorIdForPersonBrukerService(Person person) {
        if (person instanceof Person.AktorId) {
            return Optional.of((Person.AktorId)person);
        }
        var aktorId = aktorOppslagClient.hentAktorId(Fnr.of(person.get())).get();
        return Optional.ofNullable(aktorId).map(Person::aktorId);
    }

    public boolean sjekKvpTilgang(String enhet) {
        if (authContextHolder.erEksternBruker() || enhet == null || authContextHolder.erSystemBruker()) {
            return true;
        }

        return veilarbPep.harVeilederTilgangTilEnhet(getInnloggetVeilederIdent(), EnhetId.of(enhet));
    }

    public Optional<Person.Fnr> getFnrForAktorId(Person.AktorId aktorId) {
        var fnr = aktorOppslagClient.hentFnr(AktorId.of(aktorId.get())).get();
        return Optional.ofNullable(fnr).map(Person::fnr);
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

    public Fnr hentFnr(AktorId aktorId) {
        return aktorOppslagClient.hentFnr(aktorId);
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
        return authContextHolder.getSubject();
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

    public Optional<Person.AktorId> getAktorIdForEksternBruker() {
        return authContextHolder.erEksternBruker()
                ? authContextHolder.getSubject().map(sub -> Person.aktorId(aktorOppslagClient.hentAktorId(Fnr.of(sub)).get()))
                : Optional.empty();
    }
}
