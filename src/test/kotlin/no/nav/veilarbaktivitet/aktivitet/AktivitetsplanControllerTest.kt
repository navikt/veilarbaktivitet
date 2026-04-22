package no.nav.veilarbaktivitet.aktivitet

import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.oversikten.OversiktenMelding
import no.nav.veilarbaktivitet.oversikten.OversiktenMeldingMedMetadataDAO
import no.nav.veilarbaktivitet.oversikten.UtsendingStatus
import no.nav.veilarbaktivitet.testUtils.AktivitetDtoTestBuilder
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
        aktivitetTestService.opprettAktivitetViaHttp(
            happyBruker, veileder, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.EGEN
            ).setOppfolgingsperiodeId(happyBruker.oppfolgingsperiodeId)
        )
    }

    @Test
    fun bruker_skal_kunne_oprette_aktivitet() {
        val happyBruker = navMockService.createBruker()
        aktivitetTestService.opprettAktivitetViaHttp(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
    }

    @Test
    fun bruker_skal_ikke_kunne_oprette_aktivitet_på_annen_bruker() {
        val happyBruker = navMockService.createBruker()
        val evilUser = navMockService.createBruker()
        val aktivitetPayloadJson = JsonUtils.toJson(AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN)
            .setOppfolgingsperiodeId(happyBruker.oppfolgingsperiodeId))
        evilUser
            .createRequest()
            .and()
            .body(aktivitetPayloadJson)
            .`when`()
            .post("http://localhost:$port/veilarbaktivitet/api/aktivitet/${happyBruker.oppfolgingsperiodeId}/ny")
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response()
        val skalVæreTom = aktivitetTestService.hentAktiviteterForFnr(happyBruker)
        Assertions.assertTrue(skalVæreTom.aktiviteter.isEmpty())
        val skalHaEnAktivitet = aktivitetTestService.hentAktiviteterForFnr(evilUser)
        Assertions.assertSame(1, skalHaEnAktivitet.aktiviteter.size)
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
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
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
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
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
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
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
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
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
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
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
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(happyBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
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
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(happyBruker, veileder, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE)
            .setOppfolgingsperiodeId(happyBruker.oppfolgingsperiodeId))
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
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE)
                .setErReferatPublisert(false)
                .setReferat(null)
                .setOppfolgingsperiodeId(happyBruker.oppfolgingsperiodeId)
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
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE)
                .setErReferatPublisert(false)
                .setOppfolgingsperiodeId(happyBruker.oppfolgingsperiodeId)
                .setReferat("Et referat")
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

        aktivitetTestService.opprettAktivitetViaHttp(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.SAMTALEREFERAT)
                .setErReferatPublisert(false)
                .setReferat("Et referat")
                .setOppfolgingsperiodeId(happyBruker.oppfolgingsperiodeId)
        )

        val antallMeldingerTilOversikten = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes().size
        assertThat(antallMeldingerTilOversikten).isEqualTo(1)
    }

    @Test
    fun når_samtalereferat_deles_med_bruker_sendes_stoppmelding_til_oversikten(){
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(happyBruker)
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.SAMTALEREFERAT)
                .setErReferatPublisert(false)
                .setReferat("Et referat")
                .setOppfolgingsperiodeId(happyBruker.oppfolgingsperiodeId)
        )
        assertThat(oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes().size).isEqualTo(1)

        aktivitetTestService.publiserReferat(aktivitet, veileder)

        val meldingerTilOversikten = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes()
        assertThat(meldingerTilOversikten.size).isEqualTo(2)
        val sisteMelding = meldingerTilOversikten.maxBy { it.opprettet }
        assertThat(sisteMelding.operasjon).isEqualTo(OversiktenMelding.Operasjon.STOPP)
    }

    @Test
    fun når_referat_på_møte_opprettes_sendes_melding_til_oversikten() {
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(happyBruker)
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE)
                .setReferat(null)
                .setErReferatPublisert(false)
                .setOppfolgingsperiodeId(happyBruker.oppfolgingsperiodeId)
        )

        aktivitet.setReferat("Et referat")
        aktivitetTestService.oppdaterReferat(aktivitet, veileder)

        val meldingerTilOversikten = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes()
        assertThat(meldingerTilOversikten.size).isEqualTo(1)
        val sisteMelding = meldingerTilOversikten.maxBy { it.opprettet }
        assertThat(sisteMelding.operasjon).isEqualTo(OversiktenMelding.Operasjon.START)
    }

    @Test
    fun `Ikke send melding til oversikten når samtalereferat deles når det opprettes`() {
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(happyBruker)

        aktivitetTestService.opprettAktivitetViaHttp(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.SAMTALEREFERAT)
                .setErReferatPublisert(true)
                .setReferat("Et referat")
                .setOppfolgingsperiodeId(happyBruker.oppfolgingsperiodeId)
        )

        val meldingerTilOversikten = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes()
        assertThat(meldingerTilOversikten).isEmpty()
    }

    @Test
    fun `Ikke send startmelding når allerede delt referat tømmes og fylles igjen`() {
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(happyBruker)

        // Opprett møte uten referat
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE)
                .setReferat(null)
                .setErReferatPublisert(false)
                .setOppfolgingsperiodeId(happyBruker.oppfolgingsperiodeId)
        )

        // Legg til referat - dette sender START-melding
        aktivitet.setReferat("Et referat")
        val aktivitetMedReferat = aktivitetTestService.oppdaterReferat(aktivitet, veileder)
        assertThat(oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes().size).isEqualTo(1)

        // Del referatet med bruker - dette sender STOPP-melding
        aktivitetTestService.publiserReferat(aktivitetMedReferat, veileder)
        val meldingerEtterDeling = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes()
        assertThat(meldingerEtterDeling.size).isEqualTo(2)
        assertThat(meldingerEtterDeling.maxBy { it.opprettet }.operasjon).isEqualTo(OversiktenMelding.Operasjon.STOPP)

        // Hent oppdatert aktivitet etter publisering
        val aktivitetEtterPublisering = aktivitetTestService.hentAktivitet(happyBruker, veileder, aktivitetMedReferat.id)

        // Tøm referatet
        aktivitetEtterPublisering.setReferat("")
        val aktivitetMedTomtReferat = aktivitetTestService.oppdaterReferat(aktivitetEtterPublisering, veileder)

        // Fyll inn nytt referat - skal IKKE sende ny START-melding siden referatet allerede er delt
        aktivitetMedTomtReferat.setReferat("Et nytt referat")
        aktivitetTestService.oppdaterReferat(aktivitetMedTomtReferat, veileder)

        // Verifiser at det fortsatt bare er 2 meldinger (START og STOPP fra før)
        val meldingerEtterNyttReferat = oversiktenMeldingMedMetadataRepository.hentAlleSomSkalSendes()
        assertThat(meldingerEtterNyttReferat.size).isEqualTo(2)
    }
}
