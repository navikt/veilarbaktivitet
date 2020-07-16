package no.nav.veilarbaktivitet.service;

import no.nav.apiapp.security.SubjectService;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.veilarbaktivitet.domain.Person;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Optional;

@Component
public class BrukerService {

    private final SubjectService subjectService = new SubjectService();

    @Inject
    private AktorService aktorService;

    public Optional<Person.AktorId> getAktorIdForPerson(Person person) {
        if (person instanceof Person.AktorId) {
            return Optional.of((Person.AktorId)person);
        }
        return aktorService.getAktorId(person.get())
                .map(Person::aktorId);
    }

    public Optional<Person.Fnr> getFNRForAktorId(Person.AktorId aktorId) {
        return aktorService.getFnr(aktorId.get())
                .map(Person::fnr);
    }

    public Optional<Person> getLoggedInnUser() {
        return subjectService.getIdentType()
                .flatMap((type) -> {
                    if (IdentType.EksternBruker.equals(type)) {
                        return getAktorIdForEksternBruker().map((id) -> (Person) id);
                    }
                    if (IdentType.InternBruker.equals(type)) {
                        return subjectService.getUserId().map(Person::navIdent);
                    }
                    if (IdentType.Systemressurs.equals(type)) {
                        return subjectService.getUserId().map(Person::navIdent);
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
        return subjectService.getUserId()
                .map(Person::fnr)
                .flatMap(this::getAktorIdForPerson);
    }
}
