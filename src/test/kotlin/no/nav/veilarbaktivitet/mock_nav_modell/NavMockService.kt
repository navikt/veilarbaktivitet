package no.nav.veilarbaktivitet.mock_nav_modell

import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.poao_tilgang_test_core.NavAnsatt
import no.nav.poao_tilgang.poao_tilgang_test_core.NavContext
import no.nav.poao_tilgang.poao_tilgang_test_core.tilgjengligeAdGrupper
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.oppfolging.periode.SisteOppfolgingsperiodeV1
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.*


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

    fun createBruker(brukerOptions: BrukerOptions = BrukerOptions.happyBruker()): MockBruker {
        return createBruker(brukerOptions, fnr = null)
    }

    fun createBruker(brukerOptions: BrukerOptions = BrukerOptions.happyBruker(), fnr: Fnr? = null): MockBruker {
        val ny = if (fnr != null) NAV_CONTEXT.privatBrukere.ny(fnr.get()) else NAV_CONTEXT.privatBrukere.ny()
        val mockBruker = MockBruker(brukerOptions, ny)

        val leggTilOppfølgingsperiode = brukerOptions.isUnderOppfolging && !brukerOptions.isOppfolgingFeiler

        if (leggTilOppfølgingsperiode) {
            val oppfolgingsperiode = mockBruker.oppfolgingsperioder.first()
            oppfolgingsperiodeService.upsertOppfolgingsperiode(
                SisteOppfolgingsperiodeV1.builder()
                    .aktorId(mockBruker.aktorId.get())
                    .uuid(oppfolgingsperiode.oppfolgingsperiodeId)
                    .startDato(oppfolgingsperiode.startTid)
                    .sluttDato(oppfolgingsperiode.sluttTid).build()
            )
        }

        WireMockUtil.stubBruker(mockBruker)
        return mockBruker
    }

    fun updateBruker(mockBruker: MockBruker, brukerOptions: BrukerOptions) {
        mockBruker.brukerOptions = brukerOptions
        WireMockUtil.stubBruker(mockBruker)
    }

    fun createVeileder(): MockVeileder {
        return createVeileder(mockBruker = null, ident = null)
    }

    fun createVeileder(mockBruker: MockBruker): MockVeileder {
        return createVeileder(mockBruker = mockBruker, ident = null)
    }

    fun createVeileder(mockBruker: MockBruker?, ident: String? = null): MockVeileder {
        val navAnsatt = if(ident != null) {
            NAV_CONTEXT.navAnsatt.get(ident)?.let {  MockVeileder(it) }
            NavAnsatt(ident)
        } else {
            NavAnsatt()
        }
        NAV_CONTEXT.navAnsatt.add(navAnsatt)
        navAnsatt.adGrupper.add(tilgjengligeAdGrupper.modiaOppfolging)

        val veileder = MockVeileder(navAnsatt)
        if (mockBruker != null) {
            veileder.addBruker(mockBruker)
        }

        return veileder
    }

    fun createVeilederMedNasjonalTilgang(): MockVeileder {
        val navAnsatt = NAV_CONTEXT.navAnsatt.nyNksAnsatt()
        return MockVeileder(navAnsatt)
    }
}
