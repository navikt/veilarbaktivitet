package no.nav.veilarbaktivitet.aktivitet

import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.oversikten.OversiktenMelding
import no.nav.veilarbaktivitet.oversikten.OversiktenMeldingMedMetadataDAO
import no.nav.veilarbaktivitet.oversikten.UtsendingStatus
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

internal class AktivitetsplanControllerTest: SpringBootTestBase() {

    @Autowired
    lateinit var oversiktenMeldingMedMetadataRepository: OversiktenMeldingMedMetadataDAO;

    @Test
    fun veileder_skal_kunne_oprette_aktivitet() {
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(happyBruker)
        aktivitetTestService.opprettAktivitet(
            happyBruker, veileder, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.EGEN
            )
        )
    }

    @Test
    fun bruker_skal_kunne_oprette_aktivitet() {
        val happyBruker = navMockService.createBruker()
        aktivitetTestService.opprettAktivitet(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
    }

    @Test
    fun bruker_skal_ikke_kunne_oprette_aktivitet_på_annen_bruker() {
        val happyBruker = navMockService.createBruker()
        val evilUser = navMockService.createBruker()
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
        val skalVæreTom = aktivitetTestService.hentAktiviteterForFnr(happyBruker)
        Assertions.assertTrue(skalVæreTom.aktiviteter.isEmpty())
        val skalHaEnAktivitet = aktivitetTestService.hentAktiviteterForFnr(evilUser)
        Assertions.assertFalse(skalHaEnAktivitet.aktiviteter.isEmpty())
    }

    @Test
    fun veileder_uten_tilgang_skal_ikke_kunne_opprette_aktiviteter_på_bruker() {
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder()
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
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder()
        veileder
            .createRequest(happyBruker)
            .get("http://localhost:$port/veilarbaktivitet/api/aktivitet")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun veileder_uten_tilgang_skal_ikke_kunne_hente_en_aktivitet() {
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder()
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
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder()
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
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder()
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
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder()
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
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder()
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
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder()
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
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(happyBruker)
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

    @Test
    fun når_udelt_referat_opprettes_så_sendes_melding_til_oversikten() {
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(happyBruker)
        val aktivitet = aktivitetTestService.opprettAktivitet(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE).setErReferatPublisert(false).setReferat("")
        )
        val oppdatertAktivitet = aktivitet
        oppdatertAktivitet.setReferat("Et referat")
        val aktivitetPayloadJson = JsonUtils.toJson(oppdatertAktivitet)

        veileder
            .createRequest(happyBruker)
            .body(aktivitetPayloadJson)
            .put("http://localhost:$port/veilarbaktivitet/api/aktivitet/${aktivitet.id}/referat")
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())

        val meldingerTilOversikten = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes()
        assertThat(meldingerTilOversikten).hasSize(1)
        val melding = meldingerTilOversikten.first()
        assertThat(melding.fnr.get()).isEqualTo(happyBruker.fnr)
        assertThat(melding.kategori).isEqualTo(OversiktenMelding.Kategori.UDELT_SAMTALEREFERAT)
        assertThat(melding.opprettet).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(melding.tidspunktSendt).isNull()
        assertThat(melding.utsendingStatus).isEqualTo(UtsendingStatus.SKAL_SENDES)
    }

    @Test
    fun skal_ikke_sende_startmelding_når_referat_oppdateres() {
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(happyBruker)
        val aktivitet = aktivitetTestService.opprettAktivitet(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE).setErReferatPublisert(false).setReferat("Et referat")
        )
        val oppdatertAktivitet = aktivitet
        oppdatertAktivitet.setReferat("Et oppdatert referat")
        val aktivitetPayloadJson = JsonUtils.toJson(oppdatertAktivitet)
        val antallMeldingerTilOversiktenFørOppdatering = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes().size

        veileder
            .createRequest(happyBruker)
            .body(aktivitetPayloadJson)
            .put("http://localhost:$port/veilarbaktivitet/api/aktivitet/${aktivitet.id}/referat")
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())

        val antallMeldingerTilOversiktenEtterOppdatering = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes().size
        assertThat(antallMeldingerTilOversiktenEtterOppdatering).isEqualTo(antallMeldingerTilOversiktenFørOppdatering)
    }

    @Test
    fun når_samtalereferat_opprettes_med_referat_sendes_melding_til_oversikten() {
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(happyBruker)

        aktivitetTestService.opprettAktivitet(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.SAMTALEREFERAT).setErReferatPublisert(false).setReferat("Et referat")
        )

        val antallMeldingerTilOversikten = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes().size
        assertThat(antallMeldingerTilOversikten).isEqualTo(1)
    }

    // TODO: Send stoppmelding når referat plutselig deles
}
