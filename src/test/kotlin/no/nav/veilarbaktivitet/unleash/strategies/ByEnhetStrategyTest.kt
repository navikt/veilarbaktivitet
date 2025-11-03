package no.nav.veilarbaktivitet.unleash.strategies

import io.getunleash.UnleashContext
import no.nav.common.client.msgraph.AdGroupData
import no.nav.common.client.msgraph.MsGraphClient
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.common.types.identer.AzureObjectId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ByEnhetStrategyTest {
    private lateinit var msGraphClient: MsGraphClient
    private lateinit var byEnhetStrategy: ByEnhetStrategy
    private lateinit var azureAdMachineToMachineTokenClient: AzureAdMachineToMachineTokenClient

    @BeforeEach
    fun setup() {
        msGraphClient = mock<MsGraphClient> {
            on { hentAdGroupsForUser(any(), any(), any()) } doReturn listOf(
                AdGroupData(AzureObjectId("1"), "0000-GA-ENHET_1111"),
                AdGroupData(AzureObjectId("2"), "0000-GA-ENHET_2222")
            )
        }
        azureAdMachineToMachineTokenClient = mock<AzureAdMachineToMachineTokenClient> {
            on { createMachineToMachineToken(any()) } doReturn "token"
        }

        byEnhetStrategy = ByEnhetStrategy(msGraphClient, azureAdMachineToMachineTokenClient, "MS-scope")
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
            UnleashContext("M123456", "", "", emptyMap()),
        )
        assertThat(enabled).isFalse()
    }

    @Test
    fun `skal returnere true når brukers enhet finnes i listen over påskrudde enheter`() {
        val enabled = byEnhetStrategy.isEnabled(
            mapOf("valgtEnhet" to "1234,1111"),
            UnleashContext("M123456", "", "", emptyMap()),
        )
        assertThat(enabled).isTrue()
    }
}