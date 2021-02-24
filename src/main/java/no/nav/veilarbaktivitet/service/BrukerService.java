package no.nav.veilarbaktivitet.service;

import no.nav.common.auth.subject.IdentType;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.veilarbaktivitet.domain.Person;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BrukerService {

    private final AktorregisterClient aktorService;

    public BrukerService(AktorregisterClient aktorService) {
        this.aktorService = aktorService;
    }

    public Optional<Person.AktorId> getAktorIdForPerson(Person person) {
        if (person instanceof Person.AktorId) {
            return Optional.of((Person.AktorId)person);
        }
        var aktorId = aktorService.hentAktorId(person.get());
        return Optional.ofNullable(aktorId).map(Person::aktorId);
    }

    public Optional<Person.Fnr> getFNR(Person person) {
        if (person instanceof Person.Fnr) {
            return Optional.of((Person.Fnr)person);
        }
        var fnr = aktorService.hentFnr(person.get());
        return Optional.ofNullable(fnr).map(Person::fnr);
    }

    public Optional<Person> getLoggedInnUser() {
        return SubjectHandler
                .getIdentType()
                .flatMap((type) -> {
                    if (IdentType.EksternBruker.equals(type)) {
                        return getAktorIdForEksternBruker().map((id) -> (Person) id);
                    }
                    if (IdentType.InternBruker.equals(type)) {
                        return SubjectHandler.getIdent().map(Person::navIdent);
                    }
                    if (IdentType.Systemressurs.equals(type)) {
                        return SubjectHandler.getIdent().map(Person::navIdent);
                    }
                    return Optional.empty();
                });
    }

    public static boolean erEksternBruker() {
        return SubjectHandler
                .getIdentType()
                .map(identType -> IdentType.EksternBruker == identType)
                .orElse(false);
    }

    public static boolean erInternBruker() {
        return SubjectHandler
                .getIdentType()
                .map(identType -> IdentType.InternBruker == identType)
                .orElse(false);
    }

    private Optional<Person.AktorId> getAktorIdForEksternBruker() {
        return SubjectHandler
                .getIdent()
                .map(Person::fnr)
                .flatMap(this::getAktorIdForPerson);
    }
}
