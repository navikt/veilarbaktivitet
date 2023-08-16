package no.nav.veilarbaktivitet.unleash

import io.getunleash.UnleashContext
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.junit.jupiter.api.Assertions.assertEquals

class FeatureControllerTest : SpringBootTestBase() {
    private val mockVeileder = MockNavService.createVeileder()

    @Test
    fun `skal returnere feature`() {
        val enabledToggle = "t1"; val disabledToggle = "t2"; val unusedToggle = "t3"

        whenever(unleash.isEnabled(eq(enabledToggle), ArgumentMatchers.any(UnleashContext::class.java))).thenReturn(true)
        whenever(unleash.isEnabled(eq(disabledToggle), ArgumentMatchers.any(UnleashContext::class.java))).thenReturn(false)

        val actual = mockVeileder.createRequest()["http://localhost:${port}/veilarbaktivitet/api/feature?feature=${enabledToggle}&feature=${disabledToggle}&feature=${unusedToggle}"]
                .then()
                .assertThat()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath().getMap(".", String::class.java, Boolean::class.java)

        assertEquals(
            mapOf(enabledToggle to true, disabledToggle to false, unusedToggle to false),
            actual
        )
    }

}