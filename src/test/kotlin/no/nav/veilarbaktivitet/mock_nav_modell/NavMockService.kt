package no.nav.veilarbaktivitet.mock_nav_modell

import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.poao_tilgang_test_core.NavAnsatt
import no.nav.poao_tilgang.poao_tilgang_test_core.tilgjengligeAdGrupper
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.oppfolging.periode.SisteOppfolgingsperiodeV1
import org.springframework.stereotype.Service


@Service
class NavMockService(
    val oppfolgingsperiodeService: OppfolgingsperiodeService,
) {

    fun createBruker(brukerOptions: BrukerOptions = BrukerOptions.happyBruker(), fnr: Fnr? = null): MockBruker {
        val bruker = MockNavService.createBruker(brukerOptions, fnr)
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

    fun createBruker(brukerOptions: BrukerOptions = BrukerOptions.happyBruker()): MockBruker {
        return createBruker(brukerOptions, null)
    }

    fun createHappyBruker(): MockBruker {
        return createBruker(BrukerOptions.happyBruker())
    }

    fun updateBruker(mockBruker: MockBruker, brukerOptions: BrukerOptions) {
        mockBruker.brukerOptions = brukerOptions
        WireMockUtil.stubBruker(mockBruker)
    }

    fun createVeileder(ident: String, mockBruker: MockBruker): MockVeileder {
        val navAnsatt = MockNavService.NAV_CONTEXT.navAnsatt.get(ident)?: NavAnsatt(ident)
        MockNavService.NAV_CONTEXT.navAnsatt.add(navAnsatt)
        navAnsatt.adGrupper.add(tilgjengligeAdGrupper.modiaOppfolging)
        val veileder = MockVeileder(navAnsatt)
        veileder.addBruker(mockBruker)
        return veileder
    }

    fun createVeileder(mockBruker: MockBruker): MockVeileder {
        val navAnsatt = NavAnsatt()
        MockNavService.NAV_CONTEXT.navAnsatt.add(navAnsatt)
        navAnsatt.adGrupper.add(tilgjengligeAdGrupper.modiaOppfolging)

        val veileder = MockVeileder(navAnsatt)
        veileder.addBruker(mockBruker)
        return veileder
    }

    fun createVeileder() :MockVeileder {
        val navAnsatt = NavAnsatt()
        MockNavService.NAV_CONTEXT.navAnsatt.add(navAnsatt)
        navAnsatt.adGrupper.add(tilgjengligeAdGrupper.modiaOppfolging)
        return MockVeileder(navAnsatt)
    }

    fun createVeilederMedNasjonalTilgang(): MockVeileder {
        val navAnsatt = MockNavService.NAV_CONTEXT.navAnsatt.nyNksAnsatt()

        return MockVeileder(navAnsatt)
    }

    fun getBrukerSomIkkeKanVarsles(): MockBruker {
        val brukerOptions = BrukerOptions.happyBruker()
            .withKanVarsles(false)

        return this.createBruker(brukerOptions)
    }
}
