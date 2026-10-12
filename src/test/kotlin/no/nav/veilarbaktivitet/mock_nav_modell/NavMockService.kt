package no.nav.veilarbaktivitet.mock_nav_modell

import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.poao_tilgang_test_core.NavAnsatt
import no.nav.poao_tilgang.poao_tilgang_test_core.NavContext
import no.nav.poao_tilgang.poao_tilgang_test_core.PrivatBruker
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

    fun createBruker(brukerOptions: BrukerOptions = BrukerOptions.happyBruker()): MockBruker {
        return createBruker(brukerOptions, fnr = null)
    }

    private fun randomFnr(): String {
        val date = (1..31).random().toString().padStart(2, '0')
        val month = (1..12).random().toString().padStart(2, '0')
        val year = (1..99).random().toString().padStart(2, '0')
        val randomDigits = (1..5).map { (0..9).random() }.joinToString("")
        return "${date}${month}${year}${randomDigits}"
    }

    fun createBruker(brukerOptions: BrukerOptions = BrukerOptions.happyBruker(), fnr: Fnr? = null): MockBruker {
        val poaoPrivatBruker = PrivatBruker(
            norskIdent = fnr?.get() ?: randomFnr(),
            erSkjermet = false,
            brukerOptions.oppfolgingsenhet,
            null
        )
        NAV_CONTEXT.privatBrukere.add(poaoPrivatBruker)
        val mockBruker = MockBruker(brukerOptions, poaoPrivatBruker)
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
        mockBruker.privatbruker.oppfolgingsenhet = brukerOptions.oppfolgingsenhet
        WireMockUtil.stubBruker(mockBruker)
    }

    fun createVeileder(): MockVeileder {
        return createVeileder(mockBruker = null, ident = null)
    }

    fun createVeileder(mockBruker: MockBruker): MockVeileder {
        return createVeileder(mockBruker = mockBruker, ident = null)
    }

    fun createVeileder(mockBruker: MockBruker?, ident: String?): MockVeileder {
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

    fun getBrukerSomIkkeKanVarsles(): MockBruker {
        val brukerOptions = BrukerOptions.happyBruker()
            .withErManuell(true)

        return this.createBruker(brukerOptions)
    }
}
