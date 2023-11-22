package no.nav.veilarbaktivitet.mock_nav_modell;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import no.nav.common.auth.context.UserRole;
import no.nav.poao_tilgang.poao_tilgang_test_core.PrivatBruker;
import no.nav.veilarbaktivitet.person.Person;

import java.util.UUID;

@Getter
public class MockBruker extends RestassuredUser {
    protected final PrivatBruker privatbruker;
    @Setter
    public UUID oppfolgingsperiode;
    @Setter(AccessLevel.PACKAGE)
    private BrukerOptions brukerOptions;

    MockBruker(BrukerOptions brukerOptions, PrivatBruker privatBruker) {
        super(privatBruker.getNorskIdent(), UserRole.EKSTERN);
        this.brukerOptions = brukerOptions;
        if (brukerOptions.isUnderOppfolging()) {
            oppfolgingsperiode = UUID.randomUUID();
        }
        this.privatbruker = privatBruker;
    }

    public String getFnr() {
        return super.ident;
    }

    public Person.AktorId getAktorId() {
        return Person.aktorId(new StringBuilder(getFnr()).reverse().toString());
    }


    public Person.Fnr getFnrAsFnr() {
        return Person.fnr(super.ident);
    }


    public Person.AktorId getAktorIdAsAktorId() {
        return getAktorId();
    }

    public String getOppfolgingsenhet() {
        return privatbruker.getOppfolgingsenhet();
    }
}
