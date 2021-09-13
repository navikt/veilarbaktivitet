package no.nav.veilarbaktivitet.mock_nav_modell;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class MockBruker {
    private final String fnr;
    private final String aktorId;
    private final String enhet;
    @Setter(AccessLevel.PACKAGE)
    private BrukerOptions brukerOptions;

    public boolean harIdent(String ident) {
        return fnr.equals(ident) || aktorId.equals(ident);
    }
}
