package no.nav.veilarbaktivitet.mock_nav_modell;


import io.restassured.specification.RequestSpecification;
import no.nav.common.auth.context.UserRole;
import no.nav.common.types.identer.NavIdent;
import no.nav.poao_tilgang.core.provider.NavEnhetTilgang;
import no.nav.poao_tilgang.poao_tilgang_test_core.DomainKt;
import no.nav.poao_tilgang.poao_tilgang_test_core.NavAnsatt;

import java.util.LinkedList;
import java.util.List;

public class MockVeileder extends RestassuredUser {
    private final NavAnsatt navAnsatt;
    private final List<MockBruker> brukerList = new LinkedList<>();

    MockVeileder(NavAnsatt navAnsatt) {
        super(navAnsatt.getNavIdent(), UserRole.INTERN);
        this.navAnsatt = navAnsatt;
    }

    public String getNavIdent() {
        return super.ident;
    }

    public RequestSpecification createRequest(MockBruker mockBruker) {
        return createRequest()
                .queryParam("fnr", mockBruker.getFnr());
    }

    public NavIdent getNavIdentAsNavident() {
        return new NavIdent(super.ident);
    }

    public void addBruker(MockBruker bruker) {
        brukerList.add(bruker);
        String oppfolgingsenhet = bruker.privatbruker.getOppfolgingsenhet();
        if(oppfolgingsenhet == null) {
            return;
        }

        navAnsatt.getEnheter().stream().filter(it -> it.getEnhetId().equals(oppfolgingsenhet)).findFirst().ifPresentOrElse(
                it -> {},
                () -> navAnsatt.getEnheter().add(new NavEnhetTilgang(oppfolgingsenhet, "enhetNavn " + oppfolgingsenhet, List.of())
        ));
    }

    public void setNasjonalTilgang(boolean nationalTilgang) {
        if(nationalTilgang) {
            navAnsatt.getAdGrupper().add(DomainKt.getTilgjengligeAdGrupper().getGosysNasjonal());
        }else {
            navAnsatt.getAdGrupper().remove(DomainKt.getTilgjengligeAdGrupper().getGosysNasjonal());
        }
    }
}
