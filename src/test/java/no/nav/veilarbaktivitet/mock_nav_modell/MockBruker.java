package no.nav.veilarbaktivitet.mock_nav_modell;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import no.nav.common.auth.context.UserRole;

@Getter
public class MockBruker extends RestassuredUser {
    private final String aktorId;
    private final String enhet;
    @Setter(AccessLevel.PACKAGE)
    private BrukerOptions brukerOptions;

    MockBruker(String fnr, String aktorId, String enhet, BrukerOptions brukerOptions) {
        super(fnr, UserRole.EKSTERN);
        this.aktorId = aktorId;
        this.enhet = enhet;
        this.brukerOptions = brukerOptions;
    }

    public String getFnr() {
        return super.ident;
    }

    public boolean harIdent(String ident) {
        return super.ident.equals(ident) || aktorId.equals(ident);
    }
}
