package no.nav.veilarbaktivitet.admin

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.common.json.JsonUtils
import no.nav.poao_tilgang.core.domain.AdGruppe
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.mock_nav_modell.WireMockUtil
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.person.Person.AktorId
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*
import java.util.List

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

        sette_oppfolgingsperiode_til_avsluttet(mockBruker.oppfolgingsperiode, mockBruker.aktorId)

        val response = veileder
            .createRequest()
            .and()
            .`when`()
            .put("http://localhost:$port/veilarbaktivitet/api/admin/avsluttoppfolgingsperiode/$oppfolgingsperiodeString?fnr=$fnr")
            .then()

        response.assertThat().statusCode(HttpStatus.SC_OK)

        val etterAvslutning = aktivitetTestService.hentAktivitet(mockBruker, aktivitet.id)

        Assertions.assertThat(etterAvslutning.isHistorisk).isTrue()
    }

    fun sette_oppfolgingsperiode_til_avsluttet(oppfolgingsperiode: UUID, aktorId: AktorId ) {
        val oppfolgingsperiode = OppfolgingPeriodeMinimalDTO(
            oppfolgingsperiode,
            WireMockUtil.GJELDENDE_OPPFOLGINGSPERIODE_MOCK_START, ZonedDateTime.now()
        )

        val oppfolgingsperioder = JsonUtils.toJson(List.of(oppfolgingsperiode))

        WireMock.stubFor(
            WireMock.get("/veilarboppfolging/api/v2/oppfolging/perioder?aktorId=" + aktorId.get())
                .willReturn(
                    WireMock.ok()
                        .withHeader("Content-Type", "text/json")
                        .withBody(oppfolgingsperioder)
                )
        )
    }

}