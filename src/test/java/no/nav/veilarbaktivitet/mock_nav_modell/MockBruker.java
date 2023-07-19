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
    @Setter(AccessLevel.PACKAGE)
    private UUID oppfolgingsperiode = UUID.randomUUID();
    @Setter(AccessLevel.PACKAGE)
    private BrukerOptions brukerOptions;

    MockBruker(BrukerOptions brukerOptions, PrivatBruker privatBruker) {
        super(privatBruker.getNorskIdent(), UserRole.EKSTERN);
        this.brukerOptions = brukerOptions;
        this.privatbruker = privatBruker;
    }


    public String getFnr() {
        return super.ident;
    }

    public String getAktorId() {
        return new StringBuilder(getFnr()).reverse().toString();
    }


    public Person.Fnr getFnrAsFnr() {
        return Person.fnr(super.ident);
    }


    public Person.AktorId getAktorIdAsAktorId() {
        return Person.aktorId(getAktorId());
    }

    public String getOppfolgingsenhet() {
        return privatbruker.getOppfolgingsenhet();
    }
}
