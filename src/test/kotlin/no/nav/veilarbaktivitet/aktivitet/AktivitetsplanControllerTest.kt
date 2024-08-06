package no.nav.veilarbaktivitet.aktivitet

import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

internal class AktivitetsplanControllerTest : SpringBootTestBase() {
    @Test
    fun veileder_skal_kunne_opprete_aktivitet() {
        val happyBruker = navMockService.createHappyBruker()
        val veileder = MockNavService.createVeileder(happyBruker)
        aktivitetTestService.opprettAktivitet(
            happyBruker, veileder, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.EGEN
            )
        )
    }

    @Test
    fun bruker_skal_kunne_opprete_aktivitet() {
        val happyBruker = navMockService.createHappyBruker()
        aktivitetTestService.opprettAktivitet(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
    }

    @Test
    fun bruker_skal_ikke_kunne_opprete_aktivitet_på_annen_bruker() {
        val happyBruker = navMockService.createHappyBruker()
        val evilUser = MockNavService.createHappyBruker()
        val aktivitetPayloadJson = JsonUtils.toJson(AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        val response = evilUser
            .createRequest()
            .and()
            .body(aktivitetPayloadJson)
            .`when`()
            .post("http://localhost:$port/veilarbaktivitet/api/aktivitet/${happyBruker.oppfolgingsperiodeId}/ny?fnr=${happyBruker.fnr}")
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response()
        val skalVereTomm = aktivitetTestService.hentAktiviteterForFnr(happyBruker)
        Assertions.assertTrue(skalVereTomm.aktiviteter.isEmpty())
        val skalHaEnAktivitet = aktivitetTestService.hentAktiviteterForFnr(evilUser)
        Assertions.assertFalse(skalHaEnAktivitet.aktiviteter.isEmpty())
    }

    @Test
    fun veileder_uten_tilgang_skal_ikke_kunne_opprette_aktiviteter_på_bruker() {
        val happyBruker = navMockService.createHappyBruker()
        val veileder = MockNavService.createVeileder()
        val aktivitetPayloadJson = JsonUtils.toJson(AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        veileder
            .createRequest(happyBruker)
            .and()
            .body(aktivitetPayloadJson)
            .`when`()
            .post("http://localhost:$port/veilarbaktivitet/api/aktivitet/${happyBruker.oppfolgingsperiodeId}/ny")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun veileder_uten_tilgang_skal_ikke_kunne_hente_aktiviteter_på_bruker() {
        val happyBruker = navMockService.createHappyBruker()
        val veileder = MockNavService.createVeileder()
        veileder
            .createRequest(happyBruker)
            .get("http://localhost:$port/veilarbaktivitet/api/aktivitet")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun veileder_uten_tilgang_skal_ikke_kunne_hente_en_aktivitet() {
        val happyBruker = navMockService.createHappyBruker()
        val veileder = MockNavService.createVeileder()
        val aktivitet = aktivitetTestService.opprettAktivitet(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        veileder
            .createRequest(happyBruker)
            .get("http://localhost:$port/veilarbaktivitet/api/aktivitet/${aktivitet.id}")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun veileder_uten_tilgang_skal_ikke_kunne_hente_aktivitetsversjoner() {
        val happyBruker = navMockService.createHappyBruker()
        val veileder = MockNavService.createVeileder()
        val aktivitet = aktivitetTestService.opprettAktivitet(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        veileder
            .createRequest(happyBruker)
            .get("http://localhost:$port/veilarbaktivitet/api/aktivitet/${aktivitet.id}/versjoner")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun veileder_uten_tilgang_skal_ikke_kunne_oppdatere_aktiviteter() {
        val happyBruker = navMockService.createHappyBruker()
        val veileder = MockNavService.createVeileder()
        val aktivitet = aktivitetTestService.opprettAktivitet(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        val aktivitetPayloadJson = JsonUtils.toJson(aktivitet)
        veileder
            .createRequest(happyBruker)
            .body(aktivitetPayloadJson)
            .put("http://localhost:$port/veilarbaktivitet/api/aktivitet/${aktivitet.id}")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun veileder_uten_tilgang_skal_ikke_kunne_oppdatere_etiketter() {
        val happyBruker = navMockService.createHappyBruker()
        val veileder = MockNavService.createVeileder()
        val aktivitet = aktivitetTestService.opprettAktivitet(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        val aktivitetPayloadJson = JsonUtils.toJson(aktivitet)
        veileder
            .createRequest(happyBruker)
            .body(aktivitetPayloadJson)
            .put("http://localhost:$port/veilarbaktivitet/api/aktivitet/${aktivitet.id}/etikett")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun veileder_uten_tilgang_skal_ikke_kunne_oppdatere_status() {
        val happyBruker = navMockService.createHappyBruker()
        val veileder = MockNavService.createVeileder()
        val aktivitet = aktivitetTestService.opprettAktivitet(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        val aktivitetPayloadJson = JsonUtils.toJson(aktivitet)
        veileder
            .createRequest(happyBruker)
            .body(aktivitetPayloadJson)
            .put("http://localhost:$port/veilarbaktivitet/api/aktivitet/${aktivitet.id}/status")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun veileder_uten_tilgang_skal_ikke_kunne_oppdatere_referat() {
        val happyBruker = navMockService.createHappyBruker()
        val veileder = MockNavService.createVeileder()
        val aktivitet = aktivitetTestService.opprettAktivitet(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        val aktivitetPayloadJson = JsonUtils.toJson(aktivitet)
        veileder
            .createRequest(happyBruker)
            .body(aktivitetPayloadJson)
            .put("http://localhost:$port/veilarbaktivitet/api/aktivitet/${aktivitet.id}/referat")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun bruker_skal_ikke_kunne_publisere_referat() {
        val happyBruker = navMockService.createHappyBruker()
        val veileder = MockNavService.createVeileder(happyBruker)
        val aktivitet = aktivitetTestService.opprettAktivitet(happyBruker, veileder, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE))
        val aktivitetPayloadJson = JsonUtils.toJson(aktivitet)
        happyBruker
            .createRequest()
            .body(aktivitetPayloadJson)
            .put("http://localhost:$port/veilarbaktivitet/api/aktivitet/${aktivitet.id}/referat/publiser")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }
}
