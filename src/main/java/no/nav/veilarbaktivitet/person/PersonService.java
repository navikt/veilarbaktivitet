package no.nav.veilarbaktivitet.person;

import lombok.RequiredArgsConstructor;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
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

    public Optional<Person.AktorId> getAktorIdForPersonBruker(Person person)  {
        if (!person.erEkstern()) {
            return Optional.empty();
        }
        if (person instanceof Person.AktorId aktorId) {
            return Optional.of(aktorId);
        }
        try {
            var aktorId = aktorOppslagClient.hentAktorId(Fnr.of(person.get())).get();
            return Optional.ofNullable(aktorId).map(Person::aktorId);
        } catch (IngenGjeldendeIdentException e) {
            return Optional.empty();
        }
    }

    public Person.Fnr getFnrForAktorId(Person.AktorId aktorId) throws IngenGjeldendeIdentException {
        return Person.fnr(aktorOppslagClient.hentFnr(AktorId.of(aktorId.get())).get());
    }

    public Optional<Person.Fnr> getFnrForPersonbruker(Person person) {
        if (!person.erEkstern()) {
            return Optional.empty();
        }

        if (person instanceof Person.Fnr fnr) {
            return Optional.of(fnr);
        }

        try {
            String fnr = aktorOppslagClient.hentFnr(AktorId.of(person.get())).get();
            return Optional.ofNullable(fnr).map(Person::fnr);
        } catch (IngenGjeldendeIdentException e) {
            return Optional.empty();
        }
    }

    @NotNull
    @Override
    public AktorId getAktorIdForPersonBruker(@NotNull EksternBrukerId eksternBrukerId) throws IngenGjeldendeIdentException {
        if (eksternBrukerId.type() == EksternBrukerId.Type.AKTOR_ID) return AktorId.of(eksternBrukerId.get());
        return aktorOppslagClient.hentAktorId(Fnr.of(eksternBrukerId.get()));
    }

    @NotNull
    @Override
    public Fnr getFnrForAktorId(@NotNull EksternBrukerId eksternBrukerId) throws IngenGjeldendeIdentException {
        if (eksternBrukerId.type() == EksternBrukerId.Type.FNR) return Fnr.of(eksternBrukerId.get());
        return aktorOppslagClient.hentFnr(AktorId.of(eksternBrukerId.get()));
    }
}
