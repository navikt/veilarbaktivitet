package no.nav.veilarbaktivitet.mock_nav_modell

import no.nav.poao_tilgang.poao_tilgang_test_core.NavAnsatt
import no.nav.poao_tilgang.poao_tilgang_test_core.tilgjengligeAdGrupper
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.oppfolging.periode.SisteOppfolgingsperiodeV1
import org.springframework.stereotype.Service


@Service
class NavMockService(
    val oppfolgingsperiodeService: OppfolgingsperiodeService,
) {

    fun createHappyBruker(brukerOptions: BrukerOptions = BrukerOptions.happyBruker()): MockBruker {
        val bruker = MockNavService.createBruker(brukerOptions)
        val oppfolgingsperiode = bruker.oppfolgingsperioder.first()
        oppfolgingsperiodeService.upsertOppfolgingsperiode(
            SisteOppfolgingsperiodeV1.builder()
                .aktorId(bruker.aktorId.get())
                .uuid(oppfolgingsperiode.oppfolgingsperiodeId)
                .startDato(oppfolgingsperiode.startTid)
                .sluttDato(oppfolgingsperiode.sluttTid).build()
        )
        return bruker
    }

    fun updateBruker(mockBruker: MockBruker, brukerOptions: BrukerOptions) {
        mockBruker.brukerOptions = brukerOptions
        WireMockUtil.stubBruker(mockBruker)
    }

    fun createVeileder(ident: String? = null, mockBruker: MockBruker): MockVeileder {
        val navAnsatt = if(ident != null) {
            MockNavService.NAV_CONTEXT.navAnsatt.get(ident)?.let { return MockVeileder(it) }
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
