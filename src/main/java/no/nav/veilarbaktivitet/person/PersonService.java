package no.nav.veilarbaktivitet.person;

import lombok.RequiredArgsConstructor;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.utils.graphql.GraphqlErrorException;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.EksternBrukerId;
import no.nav.common.types.identer.Fnr;
import no.nav.poao.dab.spring_auth.IPersonService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PersonService implements IPersonService {
    private final AktorOppslagClient aktorOppslagClient;

    public Optional<Person.AktorId> getAktorIdForPersonBruker(Person person) throws IkkeFunnetPersonException {
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

    public Person.Fnr getFnrForAktorId(Person.AktorId aktorId) throws IkkeFunnetPersonException, UgyldigIdentException {
        return getFnrForPersonbruker(aktorId).orElseThrow(() -> new RuntimeException("aktorOppslagClient skal aldri returnere null"));
    }

    public Optional<Person.Fnr> getFnrForPersonbruker(Person person) throws IkkeFunnetPersonException, UgyldigIdentException {
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
        return e.getErrors().stream().anyMatch(error -> error.getMessage().equals("Fant ikke person"));
    }
    private boolean isUgyldigIdentError(GraphqlErrorException e) {
        /* Fra loggen
        [GraphQLError(message=Ugyldig ident, locations=[Location(line=3, column=5)], path=[hentIdenter], extensions={code=bad_request, id=ugyldig_ident, classification=ValidationError})]         */
        return e.getErrors().stream().anyMatch(error -> error.getMessage().equals("Ugyldig ident"));
    }
    private RuntimeException mapException(GraphqlErrorException e, Person person) {
        if (isFantIkkePersonError(e)) {
            if (person instanceof Person.AktorId) {
                return new IkkeFunnetPersonException("Fant ikke person for aktørId=" + person.get());
            } else {
                return new IkkeFunnetPersonException("Fant ikke person for fnr=" + person.get());
            }
        } else if (isUgyldigIdentError(e)) {
            return new UgyldigIdentException("Ugyldig ident: " + person.get());
        } else {
                return e;
        }
    }

    @NotNull
    @Override
    public AktorId getAktorIdForPersonBruker(@NotNull EksternBrukerId eksternBrukerId) {
        return getAktorIdForPersonBruker(Person.of(eksternBrukerId))
                .orElseThrow(IkkeFunnetPersonException::new)
                .otherAktorId();
    }

    @NotNull
    @Override
    public Fnr getFnrForAktorId(@NotNull EksternBrukerId eksternBrukerId) {
        return getFnrForPersonbruker(Person.of(eksternBrukerId))
                .orElseThrow(IkkeFunnetPersonException::new)
                .otherFnr();
    }
}
