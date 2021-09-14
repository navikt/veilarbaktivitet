package no.nav.veilarbaktivitet.mock_nav_modell;


import lombok.Getter;
import lombok.Setter;
import no.nav.common.auth.context.UserRole;

import java.util.LinkedList;
import java.util.List;

public class MockVeileder extends RestassureddUser {
    @Setter
    @Getter
    private boolean nationalTilgang = false;
    private final List<MockBruker> brukerList = new LinkedList<>();

    MockVeileder(String ident) {
        super(ident, UserRole.INTERN);
    }

    public String getNavIdent() {
        return super.ident;
    }

    public void addBruker(MockBruker bruker) {
        brukerList.add(bruker);
    }

    public boolean harTilgangTilBruker(MockBruker bruker) {
        return nationalTilgang || brukerList.stream().anyMatch(it -> it.equals(bruker));
    }

    public boolean harTilgagnTilenhet(String enhet) {
        return brukerList.stream().anyMatch(it -> it.getEnhet().equals(enhet));
    }
}
