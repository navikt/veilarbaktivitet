package no.nav.veilarbaktivitet.service;

import no.nav.common.abac.Pep;
import no.nav.common.abac.domain.AbacPersonId;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.Subject;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.veilarbaktivitet.domain.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AktorregisterClient aktorregisterClient;

    private final Pep veilarbPep;

    @Autowired
    public AuthService(AktorregisterClient aktorregisterClient, Pep veilarbPep) {
        this.aktorregisterClient = aktorregisterClient;
        this.veilarbPep = veilarbPep;
    }


    public void sjekkTilgangTilPerson(Person ident) {
        String aktorId = getAktorIdForPerson(ident);
        String innloggetBrukerToken = SubjectHandler.getSsoToken()
                .map(SsoToken::getToken)
                .orElseThrow(() -> new IllegalStateException("Fant ikke token til innlogget bruker"));

        if (!veilarbPep.harTilgangTilPerson(innloggetBrukerToken, ActionId.READ, AbacPersonId.aktorId(aktorId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public void sjekkTilgangOgInternBruker(String aktorid, String enhet) {
        if(!BrukerService.erInternBruker()) {
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
            return aktorregisterClient.hentAktorId(person.get());
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    public boolean sjekKvpTilgang(String enhet) {
        if (BrukerService.erEksternBruker() || enhet == null) {
            return true;
        }

        return veilarbPep.harVeilederTilgangTilEnhet(getInnloggetVeilederIdent(), enhet);
    }

    public void sjekkVeilederHarSkriveTilgangTilPerson(String aktorId) {
        boolean harTilgang = veilarbPep.harVeilederTilgangTilPerson(getInnloggetVeilederIdent(), ActionId.WRITE, AbacPersonId.aktorId(aktorId));
        if (!harTilgang) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public String getInnloggetVeilederIdent() {
        return SubjectHandler
                .getSubject()
                .filter(subject -> IdentType.InternBruker.equals(subject.getIdentType()))
                .map(Subject::getUid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fant ikke ident for innlogget veileder"));
    }


}
