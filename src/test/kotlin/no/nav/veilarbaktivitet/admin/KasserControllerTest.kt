package no.nav.veilarbaktivitet.admin

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

internal class KasserControllerTest : SpringBootTestBase() {
    private val mockBruker by lazy { navMockService.createHappyBruker(BrukerOptions.happyBruker()) }
    private val veileder by lazy { navMockService.createVeileder(ident = "Z999999", mockBruker = mockBruker) }

    @Test
    fun skal_ikke_kunne_kassere_aktivitet_uten_tilgang() {
        val aktivitet = aktivitetTestService.opprettAktivitet(
            mockBruker, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.STILLING
            )
        )

        veileder
            .createRequest()
            .put("http://localhost:" + port + "/veilarbaktivitet/api/kassering/" + aktivitet.id)
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun skal_lage_ny_versjon_av_aktivitet_ved_kassering() {
        val aktivitet = aktivitetTestService.opprettAktivitet(
            mockBruker, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.STILLING
            )
        )
        val opprettetAktivitet = aktivitetTestService.opprettAktivitet(mockBruker, aktivitet)
        val aktivitetId = opprettetAktivitet.id.toLong()
        val historikkFørKassering = historikkService.hentHistorikk(listOf(aktivitetId))[aktivitetId]!!
        assertThat(historikkFørKassering.endringer.size).isEqualTo(1)

        aktivitetTestService.kasserAktivitetskort(veileder, opprettetAktivitet.id)

        val historikkEtterKassering = historikkService.hentHistorikk(listOf(aktivitetId))[aktivitetId]!!
        assertThat(historikkEtterKassering.endringer.size).isEqualTo(2)
    }
}
