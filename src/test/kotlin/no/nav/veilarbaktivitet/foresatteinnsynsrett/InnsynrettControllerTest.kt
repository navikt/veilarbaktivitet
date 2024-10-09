package no.nav.veilarbaktivitet.foresatteinnsynsrett

import no.nav.common.types.identer.Fnr
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.person.FødselsnummerType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


import java.time.LocalDate


internal class InnsynrettControllerTest : SpringBootTestBase() {
    @Test
    fun `for bruker som er over 18 har ikke foresatte innsynsrett`() {
        val fødselsdato = LocalDate.now().minusYears(18)
        val bruker = navMockService.createHappyBruker(BrukerOptions.happyBruker(), Fnr.of("${fødselsdato.tilFødselsDato()}60000"))

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
        val bruker = navMockService.createHappyBruker(BrukerOptions.happyBruker(), Fnr.of("${fødselsdato.tilFødselsDato()}60000"))

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
    fun `for bruker som er født på 1900 tallet har foresatte aldri innsynsrett`() {
        val bruker = navMockService.createHappyBruker(BrukerOptions.happyBruker(), Fnr.of("01017120000"))


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
    fun `skal støtte syntetiske føldselsnumre fra TestNorge`() {
        val fødselsdato = LocalDate.now().minusYears(18).plusDays(1)
        val bruker = navMockService.createHappyBruker(BrukerOptions.happyBruker(), Fnr.of("${fødselsdato.tilSyntetiskFødselsdato(FødselsnummerType.TEST_NORGE)}50000"))


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
    fun `skal støtte syntetiske føldselsnumre fra Dolly`() {
        val fødselsdato = LocalDate.now().minusYears(18).plusDays(1)
        val bruker = navMockService.createHappyBruker(BrukerOptions.happyBruker(), Fnr.of("${fødselsdato.tilSyntetiskFødselsdato(FødselsnummerType.DOLLY)}40000"))


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
    fun `skal kunne se at en person er født på 1900-tallet selv om personnummeret starter på 9`() {
        val bruker = navMockService.createHappyBruker(BrukerOptions.happyBruker(), Fnr.of("16917197656"))


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
    fun `Veileder skal også kunne sjekke om foresatte har innsynsrett`() {
        val bruker = navMockService.createHappyBruker()
        val veileder = navMockService.createVeileder(mockBruker = bruker)
        
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

    fun LocalDate.tilSyntetiskFødselsdato(type: FødselsnummerType): String {
        val datoString = this.tilFødselsDato()
        val oppjusterFørsteMånedssifferMed =
            if (type == FødselsnummerType.TEST_NORGE) 8
            else if (type == FødselsnummerType.D_NUMMER) 4
            else 0

        val førsteMånedSiffer =
            Integer.parseInt(datoString.get(2).toString()) + oppjusterFørsteMånedssifferMed
        return datoString.replaceRange(2, 3, førsteMånedSiffer.toString())
    }
}