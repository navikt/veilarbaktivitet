package no.nav.veilarbaktivitet.controller

import no.nav.common.types.identer.NavIdent
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.AktivitetsOpprettelse
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.AktivitetsOpprettelseUtil.tilAktivitetsData
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.SporingsData
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.spesialEndringer.StatusEndring
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.aktivitet.dto.EtikettTypeDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavDTO
import no.nav.veilarbaktivitet.avtalt_med_nav.AvtaltMedNavService
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO
import no.nav.veilarbaktivitet.avtalt_med_nav.Type
import no.nav.veilarbaktivitet.db.DbTestUtils
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder.nyStillingFraNav
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder.nyStillingFraNavMedCVKanDeles
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder.nyttStillingssok
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest.collection.IsCollectionWithSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import java.time.ZonedDateTime
import java.util.*

/**
 * Aktivitetsplan interaksjoner der pålogget bruker er saksbehandler
 */
internal class AktivitetsplanRSTest : SpringBootTestBase() {
    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null

    @Autowired
    private val aktivitetDAO: AktivitetDAO? = null

    @Autowired
    private val aktivitetService: AktivitetService? = null

    @Autowired
    private val avtaltMedNavService: AvtaltMedNavService? = null

    private var orignalAktivitet: AktivitetDTO? = null
    private val nyAktivitetStatus = AktivitetStatus.AVBRUTT
    private val nyAktivitetEtikett = EtikettTypeDTO.AVSLAG
    private var lagredeAktivitetsIder: MutableList<Long?>? = null

    private var aktivitet: AktivitetDTO? = null
    private var mockBruker: MockBruker? = null
    private var mockBrukersVeileder: MockVeileder? = null
    private var annenMockVeilederMedNasjonalTilgang: MockVeileder? = null
    private var aktivVeileder: MockVeileder? = null


    @BeforeEach
    fun moreSettup() {
        mockBruker = navMockService.createBruker(BrukerOptions.happyBruker())
        mockBrukersVeileder = navMockService.createVeileder(mockBruker!!)
        annenMockVeilederMedNasjonalTilgang = navMockService.createVeilederMedNasjonalTilgang()
        aktivVeileder = mockBrukersVeileder
    }

    @AfterEach
    fun cleanup() {
        DbTestUtils.cleanupTestDb(jdbcTemplate)
    }


    @Test
    fun hentAktivitetVersjoner_returnererIkkeForhaandsorientering() {
        val aktivitet = aktivitetDAO!!.opprettNyAktivitet(
            tilAktivitetsData(
                nyttStillingssok(mockBruker!!.aktorId, mockBruker!!.getOppfolgingsperiodeId())
            )
        )
        val aktivitetId = aktivitet.getId()
        val statusEndring = StatusEndring(
            aktivitet.getId(),
            aktivitet.getVersjon(),
            SporingsData(
                aktivitet.getEndretAv(),
                Innsender.NAV,
                ZonedDateTime.now()
            ),
            AktivitetStatus.GJENNOMFORES,
            null
        )
        aktivitetService!!.oppdaterStatus(aktivitet, statusEndring)
        val sisteAktivitetVersjon = aktivitetService.hentAktivitetMedForhaandsorientering(aktivitetId)
        val fho = ForhaandsorienteringDTO.builder().tekst("fho tekst").type(Type.SEND_FORHAANDSORIENTERING).build()
        avtaltMedNavService!!.opprettFHO(
            AvtaltMedNavDTO().setAktivitetVersjon(sisteAktivitetVersjon.versjon).setForhaandsorientering(fho),
            aktivitetId,
            mockBruker!!.aktorId,
            NavIdent.of("V123")
        )
        val resultat = aktivitetTestService.hentVersjoner(aktivitetId.toString(), mockBruker, mockBrukersVeileder)


        Assertions.assertEquals(3, resultat.size)
        Assertions.assertEquals(AktivitetTransaksjonsType.AVTALT, resultat[0]!!.transaksjonsType)
        Assertions.assertNull(resultat[0]!!.forhaandsorientering)
        Assertions.assertNull(resultat[1]!!.forhaandsorientering)
        Assertions.assertNull(resultat[2]!!.forhaandsorientering)
    }

    @Test
    fun hentAktivitetsplan_henterAktiviteterMedForhaandsorientering() {
        val aktivitetDataMedForhaandsorientering = aktivitetDAO!!.opprettNyAktivitet(
            tilAktivitetsData(
                nyttStillingssok(
                    mockBruker!!.aktorId,
                    mockBruker!!.getOppfolgingsperiodeId())
            )

        )
        val aktivitetDataUtenForhaandsorientering = aktivitetDAO.opprettNyAktivitet(
            tilAktivitetsData(
            nyttStillingssok(
                mockBruker!!.aktorId,
                mockBruker!!.getOppfolgingsperiodeId())
            )
        )

        val fho = ForhaandsorienteringDTO.builder().tekst("fho tekst").type(Type.SEND_FORHAANDSORIENTERING).build()
        avtaltMedNavService!!.opprettFHO(
            AvtaltMedNavDTO().setAktivitetVersjon(aktivitetDataMedForhaandsorientering.versjon)
                .setForhaandsorientering(fho),
            aktivitetDataMedForhaandsorientering.id,
            mockBruker!!.aktorId,
            NavIdent.of("V123")
        )
        val aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockBrukersVeileder).getAktiviteter()
        Assertions.assertNotNull(
            aktiviteter.first { aktivitet ->
                aktivitet.id == aktivitetDataMedForhaandsorientering.id.toString()
            }.forhaandsorientering
        )
        Assertions.assertNull(
            aktiviteter.first { aktivitet ->
                aktivitet.id == aktivitetDataUtenForhaandsorientering.id.toString()
            }.forhaandsorientering
        )
    }

    @Test
    fun hentAktivitetsplan_henterStillingFraNavDataUtenCVData() {
        val aktivitet = nyStillingFraNav(mockBruker!!.aktorId, mockBruker!!.getOppfolgingsperiodeId())
        val aktivitetData = aktivitetDAO!!.opprettNyAktivitet(tilAktivitetsData(aktivitet))

        val resultat = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockBrukersVeileder)
        val resultatAktivitet = resultat.getAktiviteter().first()
        Assertions.assertEquals(1, resultat.getAktiviteter().size)
        Assertions.assertEquals(aktivitetData.id.toString(), resultatAktivitet.id)
        Assertions.assertNull(resultatAktivitet.stillingFraNavData.getCvKanDelesData())
    }

    @Test
    fun hentAktivitetsplan_henterStillingFraNavDataMedCVData() {
        val aktivitet = nyStillingFraNavMedCVKanDeles(mockBruker!!.aktorId, mockBruker!!.getOppfolgingsperiodeId())
        val aktivitetData = aktivitetDAO!!.opprettNyAktivitet(tilAktivitetsData(aktivitet))

        val resultat = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockBrukersVeileder)
        val resultatAktivitet: AktivitetDTO = resultat.getAktiviteter().first()
        Assertions.assertEquals(1, resultat.getAktiviteter().size)
        Assertions.assertEquals(aktivitetData.id.toString(), resultatAktivitet.id)
        Assertions.assertNotNull(resultatAktivitet.stillingFraNavData.getCvKanDelesData())
        Assertions.assertTrue(resultatAktivitet.stillingFraNavData.getCvKanDelesData().getKanDeles())
    }

    @Test
    fun hentAktivitetsplan_henterStillingFraNavDataMedCvSvar() {
        val aktivitet = nyStillingFraNavMedCVKanDeles(mockBruker!!.aktorId, mockBruker!!.getOppfolgingsperiodeId())
        val aktivitetData = aktivitetDAO!!.opprettNyAktivitet(tilAktivitetsData(aktivitet))

        val resultat = aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockBrukersVeileder)
        val resultatAktivitet: AktivitetDTO = resultat.getAktiviteter().first()
        Assertions.assertEquals(1, resultat.getAktiviteter().size)
        Assertions.assertEquals(aktivitetData.getId().toString(), resultatAktivitet.getId())
        Assertions.assertTrue(resultatAktivitet.getStillingFraNavData().getCvKanDelesData().getKanDeles())
    }

    @Test
    fun hent_aktivitsplan() {
        gitt_at_jeg_har_aktiviter()
        og_veileder_har_tilgang_til_brukers_enhet()
        da_skal_disse_aktivitene_ligge_i_min_aktivitetsplan()
    }

    @Test
    fun hent_aktivitet() {
        gitt_at_jeg_har_aktiviter()
        da_skal_jeg_kunne_hente_en_aktivitet()
    }

    @Test
    fun hent_aktivitetsplan_med_kontorsperre() {
        gitt_at_jeg_har_aktiviteter_med_kontorsperre()
        og_veileder_har_tilgang_til_brukers_enhet()
        da_aktiviteter_med_og_uten_kontosperre_ligge_i_min_aktivitetsplan()
    }

    @Test
    fun hent_aktivitet_med_kontorsperre() {
        gitt_at_jeg_har_en_aktivitet_med_kontorsperre()
        og_veileder_har_nasjonsal_tilgang_men_ikke_tilgang_til_brukers_enhet()
        da_skal_jeg_ikke_kunne_hente_noen_aktiviteter()
    }

    @Test
    fun opprett_aktivitet() {
        gitt_at_jeg_har_laget_en_aktivtet()
        nar_jeg_lagrer_aktivteten()
        da_skal_jeg_denne_aktiviteten_ligge_i_min_aktivitetsplan()
    }

    @Test
    fun oppdater_status() {
        gitt_at_jeg_har_aktiviter()
        nar_jeg_flytter_en_aktivitet_til_en_annen_status()
        da_skal_min_aktivitet_fatt_ny_status()
    }

    @Test
    fun oppdater_etikett() {
        gitt_at_jeg_har_aktiviter()
        nar_jeg_oppdaterer_etiketten_pa_en_aktivitet()
        da_skal_min_aktivitet_fatt_ny_etikett()
    }

    @Test
    fun hent_aktivitet_versjoner() {
        gitt_at_jeg_har_aktiviter()
        nar_jeg_flytter_en_aktivitet_til_en_annen_status()
        nar_jeg_henter_versjoner_pa_denne_aktiviten()
        da_skal_jeg_fa_versjonene_pa_denne_aktiviteten()
    }

    @Test
    fun oppdater_aktivtet() {
        gitt_at_jeg_har_aktiviter()
        nar_jeg_oppdaterer_en_av_aktiviten()
        da_skal_jeg_aktiviten_vare_endret()
    }

    @Test
    fun skal_ikke_kunne_endre_annet_enn_frist_pa_avtalte_aktiviter() {
        gitt_at_jeg_har_laget_en_aktivtet()
        nar_jeg_lagrer_aktivteten()
        gitt_at_jeg_har_satt_aktiviteten_til_avtalt()
        nar_jeg_oppdaterer_aktiviten()
        da_skal_kun_fristen_og_versjonen_og_etikett_vare_oppdatert()
    }

    private fun gitt_at_jeg_har_aktiviter() {
        val aktiviter = listOf(
            nyttStillingssok(mockBruker!!.aktorId, mockBruker!!.getOppfolgingsperiodeId()),
            nyttStillingssok(mockBruker!!.aktorId, mockBruker!!.getOppfolgingsperiodeId())
        )
        gitt_at_jeg_har_folgende_aktiviteter(aktiviter)
    }

    private fun gitt_at_jeg_har_aktiviteter_med_kontorsperre() {
        val enableKvp = mockBruker!!.getBrukerOptions().toBuilder().erUnderKvp(true).build()
        navMockService.updateBruker(mockBruker!!, enableKvp)
        gitt_at_jeg_har_folgende_aktiviteter(
            listOf(
                nyttStillingssok(mockBruker!!.aktorId, mockBruker!!.getOppfolgingsperiodeId()),
                nyttStillingssok(mockBruker!!.aktorId, mockBruker!!.getOppfolgingsperiodeId())
            )
        )
        val removeKvp = mockBruker!!.getBrukerOptions().toBuilder().erUnderKvp(false).build()
        navMockService.updateBruker(mockBruker!!, removeKvp)
        gitt_at_jeg_har_folgende_aktiviteter(
            listOf(
                nyttStillingssok(mockBruker!!.aktorId, mockBruker!!.getOppfolgingsperiodeId()),
                nyttStillingssok(mockBruker!!.aktorId, mockBruker!!.getOppfolgingsperiodeId())
            )
        )
    }

    private fun gitt_at_jeg_har_en_aktivitet_med_kontorsperre() {
        val enableKvp = mockBruker!!.brukerOptions.toBuilder().erUnderKvp(true).build()
        navMockService.updateBruker(mockBruker!!, enableKvp)
        gitt_at_jeg_har_folgende_aktiviteter(listOf(nyttStillingssok(
            mockBruker!!.aktorId,
            mockBruker!!.getOppfolgingsperiodeId(),
            "2121"
        )))
        val removeKvp = mockBruker!!.brukerOptions.toBuilder().erUnderKvp(false).build()
        navMockService.updateBruker(mockBruker!!, removeKvp)
    }

    private fun gitt_at_jeg_har_folgende_aktiviteter(aktiviteter: kotlin.collections.List<AktivitetsOpprettelse>) {
        lagredeAktivitetsIder = aktiviteter
            .map { aktivitet -> aktivitetService!!.opprettAktivitetIDB(aktivitet) }
            .map { it.id }
            .toMutableList()
    }

    private fun gitt_at_jeg_har_laget_en_aktivtet() {
        aktivitet = nyAktivitet()
    }

    private fun gitt_at_jeg_har_satt_aktiviteten_til_avtalt() {
        orignalAktivitet = aktivitet!!.toBuilder().build()
//        aktivitet!!.setAvtalt(true)

        val fho = ForhaandsorienteringDTO(
            null,
            Type.SEND_FORHAANDSORIENTERING,
            "Hei",
            null
        )

        val validatableResponse = aktivitetTestService.opprettFHOForInternAktivitetRequest(
            mockBruker!!, mockBrukersVeileder, AvtaltMedNavDTO()
                .setAktivitetVersjon(aktivitet!!.versjon.toLong())
                .setForhaandsorientering(fho),
            aktivitet!!.id.toLong()
        )

        val response = validatableResponse
            .assertThat().statusCode(HttpStatus.OK.value())
            .extract()
            .response()

        aktivitet = response.`as`(AktivitetDTO::class.java)
    }

    private fun nar_jeg_lagrer_aktivteten() {
        aktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBruker, mockBrukersVeileder, aktivitet)
    }

    private fun nar_jeg_oppdaterer_aktiviten() {
        orignalAktivitet = aktivitet!!.toBuilder().build()

        val validatableResponse = aktivitetTestService.oppdaterAktivitetViaHttp(
            mockBruker, mockBrukersVeileder,
            aktivitet!!.setBeskrivelse("noe tull")
                .setArbeidsgiver("Justice league")
                .setEtikett(EtikettTypeDTO.AVSLAG)
                .setTilDato(Date())
        )

        val response = validatableResponse
            .assertThat().statusCode(HttpStatus.OK.value())
            .extract()
            .response()

        aktivitet = response.`as`(AktivitetDTO::class.java)
    }

    private fun nar_jeg_flytter_en_aktivitet_til_en_annen_status() {
        val aktivitet: AktivitetDTO =
            aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockBrukersVeileder).getAktiviteter().first()
        this.aktivitet =
            aktivitetTestService.oppdaterAktivitetStatus(mockBruker, mockBrukersVeileder, aktivitet, nyAktivitetStatus)
    }

    private fun nar_jeg_oppdaterer_etiketten_pa_en_aktivitet() {
        val aktivitet: AktivitetDTO =
            aktivitetTestService.hentAktiviteterForFnr(mockBruker, mockBrukersVeileder).getAktiviteter().first()
        this.aktivitet = aktivitetTestService.oppdaterAktivitetEtikett(
            mockBruker,
            mockBrukersVeileder,
            aktivitet,
            nyAktivitetEtikett
        )
    }

    private var versjoner: MutableList<AktivitetDTO?>? = null

    private fun nar_jeg_henter_versjoner_pa_denne_aktiviten() {
        versjoner = aktivitetTestService.hentVersjoner(aktivitet!!.getId(), mockBruker, mockBrukersVeileder)
    }

    private var nyLenke: String? = null
    private var nyAvsluttetKommentar: String? = null
    private var oldOpprettetDato: Date? = null

    private fun nar_jeg_oppdaterer_en_av_aktiviten() {
        val originalAktivitet =
            aktivitetService!!.hentAktivitetMedForhaandsorientering(lagredeAktivitetsIder!!.first()!!)
        oldOpprettetDato = originalAktivitet.opprettetDato
        nyLenke = "itsOver9000.com"

        val nyAktivitet = originalAktivitet
            .toBuilder()
            .lenke(nyLenke)
            .build()

        this.aktivitet = aktivitetTestService.oppdaterAktivitetOk(
            mockBruker,
            mockBrukersVeileder,
            AktivitetDTOMapper.mapTilAktivitetDTO(nyAktivitet, false)
        )
        this.lagredeAktivitetsIder!!.set(0, this.aktivitet!!.getId().toLong())
    }


    private fun da_skal_disse_aktivitene_ligge_i_min_aktivitetsplan() {
        val aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker, aktivVeileder).getAktiviteter()
        MatcherAssert.assertThat(
            aktiviteter,
            IsCollectionWithSize.hasSize<AktivitetDTO?>(2)
        )
    }

    private fun da_aktiviteter_med_og_uten_kontosperre_ligge_i_min_aktivitetsplan() {
        val aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker, aktivVeileder).getAktiviteter()
        MatcherAssert.assertThat(
            aktiviteter,
            IsCollectionWithSize.hasSize(4)
        )
    }

    private fun da_skal_jeg_ikke_kunne_hente_noen_aktiviteter() {
        val aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker, aktivVeileder).getAktiviteter()
        MatcherAssert.assertThat(
            aktiviteter,
            IsCollectionWithSize.hasSize(0)
        )
    }

    private fun da_skal_jeg_kunne_hente_en_aktivitet() {
        MatcherAssert.assertThat(
            lagredeAktivitetsIder!!.first().toString(),
            Matchers.equalTo(
                (aktivitetTestService.hentAktivitet(
                    mockBruker,
                    mockBrukersVeileder,
                    lagredeAktivitetsIder!!.first().toString()
                )).getId()
            )
        )
    }

    private fun da_skal_jeg_denne_aktiviteten_ligge_i_min_aktivitetsplan() {
        MatcherAssert.assertThat(
            aktivitetService!!.hentAktiviteterForAktorId(mockBruker!!.getAktorId()),
            IsCollectionWithSize.hasSize(1)
        )
    }

    private fun da_skal_min_aktivitet_fatt_ny_status() {
        MatcherAssert.assertThat(
            aktivitet!!.status,
            Matchers.equalTo(nyAktivitetStatus)
        )
        MatcherAssert.assertThat(
            aktivitetService!!.hentAktivitetMedForhaandsorientering(
                aktivitet!!.id.toLong()
            ).getStatus(), Matchers.equalTo(nyAktivitetStatus)
        )
    }

    private fun da_skal_min_aktivitet_fatt_ny_etikett() {
        MatcherAssert.assertThat<EtikettTypeDTO?>(
            aktivitet!!.getEtikett(),
            Matchers.equalTo<EtikettTypeDTO?>(nyAktivitetEtikett)
        )
    }

    private fun da_skal_jeg_fa_versjonene_pa_denne_aktiviteten() {
        MatcherAssert.assertThat<MutableList<AktivitetDTO?>?>(versjoner, IsCollectionWithSize.hasSize<AktivitetDTO?>(2))
    }

    private fun da_skal_jeg_aktiviten_vare_endret() {
        val lagretAktivitet = aktivitetTestService.hentAktivitet(
            mockBruker,
            mockBrukersVeileder,
            lagredeAktivitetsIder!!.first().toString()
        )

        MatcherAssert.assertThat(lagretAktivitet.lenke, Matchers.equalTo<String?>(nyLenke))
        MatcherAssert.assertThat(
            lagretAktivitet.avsluttetKommentar,
            Matchers.equalTo<String?>(nyAvsluttetKommentar)
        )
        MatcherAssert.assertThat(lagretAktivitet.getOpprettetDato(), Matchers.equalTo<Date?>(oldOpprettetDato))
    }

    private fun da_skal_kun_fristen_og_versjonen_og_etikett_vare_oppdatert() {
        MatcherAssert.assertThat<AktivitetDTO?>(
            aktivitet, Matchers.equalTo<AktivitetDTO?>(
                orignalAktivitet!!
                    .setTilDato(aktivitet!!.tilDato)
                    .setVersjon(aktivitet!!.versjon) //automatiske felter satt av systemet
                    .setEndretAvType(aktivitet!!.endretAvType)
                    .setTransaksjonsType(aktivitet!!.transaksjonsType)
                    .setEndretDato(aktivitet!!.endretDato)
                    .setEndretAv(mockBrukersVeileder!!.navIdent)
                    .setOppfolgingsperiodeId(aktivitet!!.oppfolgingsperiodeId)
            )
        )
    }

    private fun og_veileder_har_tilgang_til_brukers_enhet() {
        aktivVeileder = mockBrukersVeileder
    }

    private fun og_veileder_har_nasjonsal_tilgang_men_ikke_tilgang_til_brukers_enhet() {
        aktivVeileder = annenMockVeilederMedNasjonalTilgang
    }

    private fun nyAktivitet(): AktivitetDTO {
        return AktivitetDTO()
            .setTittel("tittel")
            .setBeskrivelse("beskr")
            .setLenke("lenke")
            .setType(AktivitetTypeDTO.STILLING)
            .setStatus(AktivitetStatus.GJENNOMFORES)
            .setFraDato(Date())
            .setTilDato(Date())
            .setKontaktperson("kontakt")
    }
}
