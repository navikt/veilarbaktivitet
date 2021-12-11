package no.nav.veilarbaktivitet.person;

import lombok.RequiredArgsConstructor;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final AktorOppslagClient aktorOppslagClient;

    public Optional<Person.AktorId> getAktorIdForPersonBruker(Person person) {
        if (!person.erEkstern()) {
            return Optional.empty();
        }

        if (person instanceof Person.AktorId) {
            return Optional.of((Person.AktorId) person);
        }

        var aktorId = aktorOppslagClient.hentAktorId(Fnr.of(person.get())).get();
        return Optional.ofNullable(aktorId).map(Person::aktorId);
    }

    public Person.Fnr getFnrForAktorId(Person.AktorId aktorId) {
        return getFnrForPersonbruker(aktorId).orElseThrow(() -> new RuntimeException("aktorOppslagClient skal aldri returnere null"));
    }

    public Optional<Person.Fnr> getFnrForPersonbruker(Person person) {
        if (!person.erEkstern()) {
            return Optional.empty();
        }

        if (person instanceof Person.Fnr) {
            return Optional.of((Person.Fnr) person);
        }

        String fnr = aktorOppslagClient.hentFnr(AktorId.of(person.get())).get();
        return Optional.ofNullable(fnr).map(Person::fnr);

    }
}
