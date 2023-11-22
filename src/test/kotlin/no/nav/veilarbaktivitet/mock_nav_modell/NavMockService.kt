package no.nav.veilarbaktivitet.mock_nav_modell

import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.oppfolging.periode.SisteOppfolgingsperiodeV1
import org.springframework.stereotype.Service


@Service
class NavMockService(
    val oppfolgingsperiodeService: OppfolgingsperiodeService,
) {

    fun createHappyBruker(): MockBruker {
        val bruker = MockNavService.createBruker(BrukerOptions.happyBruker())
        val oppfolgingsperiode = bruker.oppfolgingsperioder.first()
        oppfolgingsperiodeService.upsertOppfolgingsperiode(
            SisteOppfolgingsperiodeV1.builder()
                .aktorId(bruker.aktorId.get())
                .uuid(oppfolgingsperiode.oppfolgingsperiode)
                .startDato(oppfolgingsperiode.startTid)
                .sluttDato(oppfolgingsperiode.sluttTid).build()
        )
        return bruker
    }

}