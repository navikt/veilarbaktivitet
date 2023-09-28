package no.nav.veilarbaktivitet.admin

import no.nav.poao_tilgang.core.domain.AdGruppe
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.apache.http.HttpStatus
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*

internal class AdminControllerTest : SpringBootTestBase() {
    private val mockBruker = MockNavService.createHappyBruker()
    private val veileder = MockNavService.createVeileder(mockBruker)
    @Test
    fun skal_ikke_kunne_avslutte_oppfolgingsperiode_uten_tilgang() {
        val aktivitet = aktivitetTestService.opprettAktivitet(
            mockBruker, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.STILLING
            )
        )

        val oppfolgingsperiodeString = aktivitet.oppfolgingsperiodeId.toString()
        val fnr = mockBruker.fnr

        val response = veileder
            .createRequest()
            .and()
            .`when`()
            .put("http://localhost:$port/veilarbaktivitet/api/admin/avsluttoppfolgingsperiode/$oppfolgingsperiodeString?fnr=$fnr")
            .then()

        response.assertThat().statusCode(HttpStatus.SC_FORBIDDEN)
    }

    @Test
    @Disabled
    fun skal_kunne_avslutte_oppfolgingsperiode_med_tilgang() {
        val aktivitet = aktivitetTestService.opprettAktivitet(
            mockBruker, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.STILLING
            )
        )

        val adminGruppe = AdGruppe(UUID.fromString("dd4462d1-fc98-478d-9b29-59802880aedd"), "DAB")
        veileder.addAdGruppe(adminGruppe)
        val oppfolgingsperiodeString = aktivitet.oppfolgingsperiodeId.toString()
        val fnr = mockBruker.fnr

        val response = veileder
            .createRequest()
            .and()
            .`when`()
            .put("http://localhost:$port/veilarbaktivitet/api/admin/avsluttoppfolgingsperiode/$oppfolgingsperiodeString?fnr=$fnr")
            .then()

        response.assertThat().statusCode(HttpStatus.SC_OK)
    }

}