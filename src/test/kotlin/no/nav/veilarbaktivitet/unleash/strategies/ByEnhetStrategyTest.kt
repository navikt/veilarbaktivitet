package no.nav.veilarbaktivitet.unleash.strategies

import io.getunleash.UnleashContext
import no.nav.common.client.axsys.AxsysClient
import no.nav.common.client.axsys.AxsysEnhet
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.NavIdent
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever

class ByEnhetStrategyTest {
    private lateinit var axsysClient: AxsysClient
    private lateinit var byEnhetStrategy: ByEnhetStrategy

    @BeforeEach
    fun setup() {
        axsysClient = Mockito.mock(AxsysClient::class.java)

        whenever(axsysClient.hentTilganger(NavIdent("M123456"))).thenReturn(listOf(
            AxsysEnhet().setNavn("Nav 111").setEnhetId(EnhetId("111")).setTemaer(listOf("OPP")),
            AxsysEnhet().setNavn("Nav 222").setEnhetId(EnhetId("222")).setTemaer(listOf("OPP")),
        ))

        byEnhetStrategy = ByEnhetStrategy(axsysClient)
    }

    @Test
    fun `skal returnere false når ingen UnleashContext er sendt inn`() {
        val enabled = byEnhetStrategy.isEnabled(mutableMapOf())
        assertThat(enabled).isFalse()
    }

    @Test
    fun `skal returnere false når brukers enhet ikke finnes i listen med påskrudde enheter`() {
        val enabled = byEnhetStrategy.isEnabled(
            mapOf("valgtEnhet" to "123,456"),
            UnleashContext("M123456", "", "", emptyMap()),
        )
        assertThat(enabled).isFalse()
    }

    @Test
    fun `skal returnere true når brukers enhet finnes i listen over påskrudde enheter`() {
        val enabled = byEnhetStrategy.isEnabled(
            mapOf("valgtEnhet" to "123,111"),
            UnleashContext("M123456", "", "", emptyMap()),
        )
        assertThat(enabled).isTrue()
    }
}