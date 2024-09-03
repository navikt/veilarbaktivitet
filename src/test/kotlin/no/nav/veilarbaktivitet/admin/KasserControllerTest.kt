package no.nav.veilarbaktivitet.admin

import no.nav.common.types.identer.NavIdent
import no.nav.common.types.identer.NorskIdent
import no.nav.common.utils.EnvironmentUtils
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.aktivitetskort.ArenaKort
import no.nav.veilarbaktivitet.aktivitetskort.arenaMeldingHeaders
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortStatus
import no.nav.veilarbaktivitet.config.ApplicationContext
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus
import java.util.*

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
        EnvironmentUtils.setProperty(ApplicationContext.VEILARB_KASSERING_IDENTER_PROPERTY, veileder.navIdent, EnvironmentUtils.Type.PUBLIC)
        val aktivitet = aktivitetTestService.opprettAktivitet(
            mockBruker, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.STILLING
            )
        )
        val opprettetAktivitet = aktivitetTestService.opprettAktivitet(mockBruker, aktivitet)
//        assertThat(opprettetAktivitet.historikk.endringer.size).isEqualTo(1)

        val kassertAktivitet = aktivitetTestService.kasserAktivitetskort(veileder, opprettetAktivitet.id)

        assertThat(kassertAktivitet.historikk.endringer.size).isEqualTo(2)
    }
}
