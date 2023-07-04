package no.nav.veilarbaktivitet.mock_nav_modell;

import no.nav.poao_tilgang.poao_tilgang_test_core.DomainKt;
import no.nav.poao_tilgang.poao_tilgang_test_core.NavAnsatt;
import no.nav.poao_tilgang.poao_tilgang_test_core.NavContext;
import no.nav.poao_tilgang.poao_tilgang_test_core.PrivatBruker;

import java.util.UUID;


public class MockNavService {
    public static final NavContext NAV_CONTEXT = new NavContext();

    public static MockBruker createHappyBruker() {
        return createBruker(BrukerOptions.happyBruker());
    }

    public static MockBruker createBruker(BrukerOptions brukerOptions) {
        PrivatBruker ny = NAV_CONTEXT.getPrivatBrukere().ny();
        MockBruker mockBruker = new MockBruker(brukerOptions, ny);
        WireMockUtil.stubBruker(mockBruker);
        return mockBruker;
    }

    public static void updateBruker(MockBruker mockBruker, BrukerOptions brukerOptions) {
        mockBruker.setBrukerOptions(brukerOptions);
        WireMockUtil.stubBruker(mockBruker);
    }

    public static void newOppfolingsperiode(MockBruker mockBruker) {
        mockBruker.setOppfolgingsperiode(UUID.randomUUID());
        WireMockUtil.stubBruker(mockBruker);
    }

    public static MockVeileder createVeileder(MockBruker... mockBruker) {
        MockVeileder veileder = createVeileder();
        for (MockBruker bruker : mockBruker) {
            veileder.addBruker(bruker);
        }
        return veileder;
    }

    public static MockVeileder createVeileder() {
        NavAnsatt navAnsatt = new NavAnsatt();
        NAV_CONTEXT.getNavAnsatt().add(navAnsatt);
        navAnsatt.getAdGrupper().add(DomainKt.getTilgjengligeAdGrupper().getModiaOppfolging());

        MockVeileder mockVeileder = new MockVeileder(navAnsatt);
        return mockVeileder;
    }

}
