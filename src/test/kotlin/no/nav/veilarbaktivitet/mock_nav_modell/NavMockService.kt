package no.nav.veilarbaktivitet.mock_nav_modell

import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.poao_tilgang_test_core.NavAnsatt
import no.nav.poao_tilgang.poao_tilgang_test_core.NavContext
import no.nav.poao_tilgang.poao_tilgang_test_core.tilgjengligeAdGrupper
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.oppfolging.periode.SisteOppfolgingsperiodeV1
import org.springframework.stereotype.Service


@Service
class NavMockService(
    val oppfolgingsperiodeService: OppfolgingsperiodeService,
) {

    companion object {
        val NAV_CONTEXT: NavContext = NavContext()
    }

    fun createHappyBruker(): MockBruker {
        return createBruker()
    }

    fun createBruker(brukerOptions: BrukerOptions = BrukerOptions.happyBruker(), fnr: Fnr? = null): MockBruker {
        val ny = if (fnr != null) NAV_CONTEXT.privatBrukere.ny(fnr.get()) else NAV_CONTEXT.privatBrukere.ny()
        val mockBruker = MockBruker(brukerOptions, ny)
        WireMockUtil.stubBruker(mockBruker)

        val oppfolgingsperiode = mockBruker.oppfolgingsperioder.first()
        oppfolgingsperiodeService.upsertOppfolgingsperiode(
            SisteOppfolgingsperiodeV1.builder()
                .aktorId(mockBruker.aktorId.get())
                .uuid(oppfolgingsperiode.oppfolgingsperiodeId)
                .startDato(oppfolgingsperiode.startTid)
                .sluttDato(oppfolgingsperiode.sluttTid).build()
        )
        return mockBruker
    }

    fun updateBruker(mockBruker: MockBruker, brukerOptions: BrukerOptions) {
        mockBruker.brukerOptions = brukerOptions
        WireMockUtil.stubBruker(mockBruker)
    }

    fun createVeileder(ident: String? = null, mockBruker: MockBruker): MockVeileder {
        val navAnsatt = if(ident != null) {
            MockNavService.NAV_CONTEXT.navAnsatt.get(ident)?.let {  MockVeileder(it) }
            NavAnsatt(ident)
        } else {
            NavAnsatt()
        }
        MockNavService.NAV_CONTEXT.navAnsatt.add(navAnsatt)
        navAnsatt.adGrupper.add(tilgjengligeAdGrupper.modiaOppfolging)

        val veileder = MockVeileder(navAnsatt)
        veileder.addBruker(mockBruker)

        return veileder
    }
}
