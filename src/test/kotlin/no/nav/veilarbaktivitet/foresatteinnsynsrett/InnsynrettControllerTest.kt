package no.nav.veilarbaktivitet.foresatteinnsynsrett

import java.time.LocalDate
import no.nav.common.types.identer.Fnr
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

internal class InnsynrettControllerTest : SpringBootTestBase() {
    @Test
    fun `for bruker som er over 18 har ikke foresatte innsynsrett`() {
        val fødselsdato = LocalDate.now().minusYears(18)
        val fnr = Fnr.of("${fødselsdato.tilFødselsDato()}60000")
        val bruker = navMockService.createBruker(BrukerOptions.happyBruker(), fnr)
        whenever(pdlFodselsdatoClient.erUnder18(eq(fnr))).thenReturn(false)

        val response = bruker
            .createRequest()
            .body(InnsynrettController.InnsynsrettInboundDTO(null))
            .post("http://localhost:$port/veilarbaktivitet/api/innsynsrett")
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .`as`(InnsynrettController.InnsynsrettOutboundDTO::class.java)

        assertThat(response.foresatteHarInnsynsrett).isFalse()
    }

    @Test
    fun `for bruker som er under 18 har foresatte innsynsrett`() {
        val fødselsdato = LocalDate.now().minusYears(18).plusDays(1)
        val fnr = Fnr.of("${fødselsdato.tilFødselsDato()}60000")
        val bruker = navMockService.createBruker(BrukerOptions.happyBruker(), fnr)
        whenever(pdlFodselsdatoClient.erUnder18(eq(fnr))).thenReturn(true)

        val response = bruker
            .createRequest()
            .body(InnsynrettController.InnsynsrettInboundDTO(null))
            .post("http://localhost:$port/veilarbaktivitet/api/innsynsrett")
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .`as`(InnsynrettController.InnsynsrettOutboundDTO::class.java)

        assertThat(response.foresatteHarInnsynsrett).isTrue()
    }
    
    @Test
    fun `Veileder skal også kunne sjekke om foresatte har innsynsrett`() {
        val bruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(mockBruker = bruker)
        whenever(pdlFodselsdatoClient.erUnder18(eq(Fnr.of(bruker.fnr)))).thenReturn(false)
        
        veileder
            .createRequest()
            .body(InnsynrettController.InnsynsrettInboundDTO(bruker.fnr))
            .post("http://localhost:$port/veilarbaktivitet/api/innsynsrett")
            .then()
            .assertThat()
            .statusCode(200)
    }

    fun LocalDate.tilFødselsDato(): String {
        val dag = this.dayOfMonth.toString().padStart(2, '0')
        val måned = this.monthValue.toString().padStart(2, '0')
        val år = this.year.toString().substring(2)
        return "$dag$måned$år"
    }
}