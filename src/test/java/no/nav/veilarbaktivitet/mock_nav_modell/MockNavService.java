package no.nav.veilarbaktivitet.mock_nav_modell;

import no.nav.common.types.identer.Fnr;
import no.nav.poao_tilgang.poao_tilgang_test_core.DomainKt;
import no.nav.poao_tilgang.poao_tilgang_test_core.NavAnsatt;
import no.nav.poao_tilgang.poao_tilgang_test_core.NavContext;
import no.nav.poao_tilgang.poao_tilgang_test_core.PrivatBruker;

import java.util.UUID;

// Gå over til NavMockService
@Deprecated
public class MockNavService {
    public static final NavContext NAV_CONTEXT = new NavContext();

    public static MockBruker createBruker(BrukerOptions brukerOptions) {
        return createBruker(brukerOptions, null);
    }

    public static MockBruker createBruker(BrukerOptions brukerOptions, Fnr fnr) {
        PrivatBruker ny = fnr != null ? NAV_CONTEXT.getPrivatBrukere().ny(fnr.get()) : NAV_CONTEXT.getPrivatBrukere().ny();
        MockBruker mockBruker = new MockBruker(brukerOptions, ny);
        WireMockUtil.stubBruker(mockBruker);
        return mockBruker;
    }

    public static void updateBruker(MockBruker mockBruker, BrukerOptions brukerOptions) {
        mockBruker.setBrukerOptions(brukerOptions);
        WireMockUtil.stubBruker(mockBruker);
    }

    public static void newOppfolingsperiode(MockBruker mockBruker) {
        mockBruker.setOppfolgingsperiodeId(UUID.randomUUID());
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

        return new MockVeileder(navAnsatt);
    }

    public static MockVeileder createVeilederMedNasjonalTilgang() {
        NavAnsatt navAnsatt = NAV_CONTEXT.getNavAnsatt().nyNksAnsatt();

        return new MockVeileder(navAnsatt);
    }

}
