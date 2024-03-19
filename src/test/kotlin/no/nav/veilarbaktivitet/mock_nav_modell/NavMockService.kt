package no.nav.veilarbaktivitet.mock_nav_modell

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

    fun createVeileder(vararg mockBruker: MockBruker): MockVeileder {
        val veileder = MockNavService.createVeileder()
        for (bruker in mockBruker) {
            veileder.addBruker(bruker)
        }
        return veileder
    }
}
