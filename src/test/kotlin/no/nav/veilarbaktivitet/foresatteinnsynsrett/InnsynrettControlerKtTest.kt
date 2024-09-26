package no.nav.veilarbaktivitet.foresatteinnsynsrett

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


import java.time.LocalDate


internal class InnsynrettControlerKtTest: SpringBootTestBase() {
    @Test
    fun `for bruker som er over 18 har ikke foresatte innsyns rett`() {
        val fødselsdato = LocalDate.now().minusYears(18)
        val brukerOptions = BrukerOptions.happyBruker().toBuilder().fnr("${fødselsdato.tilFødselsDato()}60000").build()
        val bruker = navMockService.createHappyBruker(brukerOptions)

        val response = bruker
            .createRequest()
            .get( "http://localhost:$port/veilarbaktivitet/api/innsynsrett")
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .`as`(InnsynrettControler.InnsynsrettDTO::class.java)

        assertThat(response.foresatteHarInnsynsrett).isFalse()
    }

    @Test
    fun `for bruker som er under 18 har foresatte innsyns rett`() {
        val fødselsdato = LocalDate.now().minusYears(18).plusDays(1)
        val brukerOptions = BrukerOptions.happyBruker().toBuilder().fnr("${fødselsdato.tilFødselsDato()}60000").build()
        val bruker = navMockService.createHappyBruker(brukerOptions)

        val response = bruker
            .createRequest()
            .get( "http://localhost:$port/veilarbaktivitet/api/innsynsrett")
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .`as`(InnsynrettControler.InnsynsrettDTO::class.java)

        assertThat(response.foresatteHarInnsynsrett).isTrue()
    }

    @Test
    fun `for bruker som er født på 1900 tallet har foresatte aldri innsyns rett`() {

        val brukerOptions = BrukerOptions.happyBruker().toBuilder().fnr("01012320000").build()
        val bruker = navMockService.createHappyBruker(brukerOptions)

        val response = bruker
            .createRequest()
            .get( "http://localhost:$port/veilarbaktivitet/api/innsynsrett")
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .response()
            .`as`(InnsynrettControler.InnsynsrettDTO::class.java)

        assertThat(response.foresatteHarInnsynsrett).isFalse()
    }

    fun LocalDate.tilFødselsDato(): String {
        val dag = this.dayOfMonth.toString().padStart(2, '0')
        val måned = this.monthValue.toString().padStart(2, '0')
        val år = this.year.toString().substring(2)
        return "$dag$måned$år"
    }
}