package no.nav.veilarbaktivitet.mock_nav_modell;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import no.nav.common.auth.context.UserRole;
import no.nav.veilarbaktivitet.person.Person;

import java.util.UUID;

@Getter
public class MockBruker extends RestassuredUser {
    private final String aktorId;
    private final UUID oppfolgingsperiode = UUID.randomUUID();
    @Setter(AccessLevel.PACKAGE)
    private BrukerOptions brukerOptions;

    MockBruker(String fnr, String aktorId, BrukerOptions brukerOptions) {
        super(fnr, UserRole.EKSTERN);
        this.aktorId = aktorId;
        this.brukerOptions = brukerOptions;
    }

    public String getFnr() {
        return super.ident;
    }

    public Person.Fnr getFnrAsFnr() {
        return Person.fnr(super.ident);
    }

    public boolean harIdent(String ident) {
        return super.ident.equals(ident) || aktorId.equals(ident);
    }

    public Person.AktorId getAktorIdAsAktorId() {
        return Person.aktorId(aktorId);
    }
}
