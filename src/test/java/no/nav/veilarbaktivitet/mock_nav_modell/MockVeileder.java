package no.nav.veilarbaktivitet.mock_nav_modell;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class MockVeileder {
    @Getter
    private final String navIdent;
    @Setter
    @Getter
    private boolean nationalTilgang = false;
    private final List<MockBruker> brukerList = new LinkedList<>();


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
