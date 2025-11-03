package no.nav.veilarbaktivitet.unleash.strategies

import io.getunleash.UnleashContext
import no.nav.poao_tilgang.client.AdGruppe
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.api.ApiResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.*

class ByEnhetStrategyTest {
    private lateinit var poaoTilgangClient: PoaoTilgangClient
    private lateinit var byEnhetStrategy: ByEnhetStrategy

    @BeforeEach
    fun setup() {
        poaoTilgangClient = mock<PoaoTilgangClient> {
            on { hentAdGrupper(any()) } doReturn ApiResult.success(listOf(
                AdGruppe(UUID.randomUUID(), "0000-GA-ENHET_1111"),
                AdGruppe(UUID.randomUUID(), "0000-GA-ENHET_2222")
            ))
        }

        byEnhetStrategy = ByEnhetStrategy(poaoTilgangClient)
    }

    @Test
    fun `skal returnere false når ingen UnleashContext er sendt inn`() {
        val enabled = byEnhetStrategy.isEnabled(mutableMapOf())
        assertThat(enabled).isFalse()
    }

    @Test
    fun `skal returnere false når brukers enhet ikke finnes i listen med påskrudde enheter`() {
        val enabled = byEnhetStrategy.isEnabled(
            mapOf("valgtEnhet" to "1234,4565"),
            UnleashContext(UUID.randomUUID().toString(), "", "", emptyMap()),
        )
        assertThat(enabled).isFalse()
    }

    @Test
    fun `skal returnere true når brukers enhet finnes i listen over påskrudde enheter`() {
        val enabled = byEnhetStrategy.isEnabled(
            mapOf("valgtEnhet" to "1234,1111"),
            UnleashContext(UUID.randomUUID().toString(), "", "", emptyMap()),
        )
        assertThat(enabled).isTrue()
    }
}