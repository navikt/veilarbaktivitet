package no.nav.veilarbaktivitet.person;

import lombok.RequiredArgsConstructor;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.utils.graphql.GraphqlErrorException;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final AktorOppslagClient aktorOppslagClient;

    public Optional<Person.AktorId> getAktorIdForPersonBruker(Person person) throws UgyldigPersonIdentException {
        if (!person.erEkstern()) {
            return Optional.empty();
        }

        if (person instanceof Person.AktorId) {
            return Optional.of((Person.AktorId) person);
        }

        try {
            var aktorId = aktorOppslagClient.hentAktorId(Fnr.of(person.get())).get();
            return Optional.ofNullable(aktorId).map(Person::aktorId);
        } catch (GraphqlErrorException e) {
            throw mapException(e, person);
        }
    }

    public Person.Fnr getFnrForAktorId(Person.AktorId aktorId) throws UgyldigPersonIdentException {
        return getFnrForPersonbruker(aktorId).orElseThrow(() -> new RuntimeException("aktorOppslagClient skal aldri returnere null"));
    }

    public Optional<Person.Fnr> getFnrForPersonbruker(Person person) throws UgyldigPersonIdentException {
        if (!person.erEkstern()) {
            return Optional.empty();
        }

        if (person instanceof Person.Fnr) {
            return Optional.of((Person.Fnr) person);
        }

        try {
            String fnr = aktorOppslagClient.hentFnr(AktorId.of(person.get())).get();
            return Optional.ofNullable(fnr).map(Person::fnr);
        } catch (GraphqlErrorException e) {
            throw mapException(e, person);
        }
    }

    private boolean isFantIkkePersonError(GraphqlErrorException e) {
        /* Fra loggen
        [{"message":"Fant ikke person","locations":[{"line":1,"column":25}],"path":["hentIdenter"],"extensions":{"code":"not_found","classification":"ExecutionAborted"}}]
         */
        return e.getErrors().stream().filter(error -> error.getMessage().equals("Fant ikke person")).findFirst().isPresent();
    }
    private RuntimeException mapException(GraphqlErrorException e, Person person) {
        if (isFantIkkePersonError(e)) {
            if (person instanceof Person.AktorId) {
                return new UgyldigPersonIdentException("Fant ikke person for aktørId=" + person.get());
            } else {
                return new UgyldigPersonIdentException("Fant ikke person for fnr=" + person.get());
            }
        } else {
            return e;
        }
    }
}
