package no.nav.veilarbaktivitet.oversikten

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.db.DbTestUtils
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

open class OversiktenMeldingMedMetadataDAOTest: SpringBootTestBase() {

    @Autowired
    lateinit var oversiktenService: OversiktenService

    @Autowired
    private lateinit var oversiktenMeldingMedMetadataDAO: OversiktenMeldingMedMetadataDAO

    @BeforeEach
    fun beforeEach() {
        DbTestUtils.cleanupTestDb(jdbcTemplate)
    }

    @Test
    fun `UdeltSamtalereferatForMøte IkkeSendtMelding ReturnererEttResultat`() {
        val (happyBruker, aktivitet) = settOppUdeltSamtalereferatUtenMelding(AktivitetTypeDTO.MOTE)

        val resultat = oversiktenMeldingMedMetadataDAO.hentUdelteSamtalereferatDerViIkkeHarSendtMeldingTilOversikten()

        assertThat(resultat).hasSize(1)
        assertThat(resultat.first().aktorId.get()).isEqualTo(happyBruker.aktorId.get())
        assertThat(resultat.first().aktivitetId.toString()).isEqualTo(aktivitet.id)
    }

    @Test
    fun `UdeltSamtalereferatForSamtalereferat IkkeSendtMelding ReturnererEttResultat`() {
        val (happyBruker, aktivitet) = settOppUdeltSamtalereferatUtenMelding(AktivitetTypeDTO.SAMTALEREFERAT)

        val resultat = oversiktenMeldingMedMetadataDAO.hentUdelteSamtalereferatDerViIkkeHarSendtMeldingTilOversikten()

        assertThat(resultat).hasSize(1)
        assertThat(resultat.first().aktorId.get()).isEqualTo(happyBruker.aktorId.get())
        assertThat(resultat.first().aktivitetId.toString()).isEqualTo(aktivitet.id)
    }

    @Test
    fun `UdeltSamtalereferatForMøte SendtStartMelding ReturnererIngen`() {
        val (happyBruker, aktivitet) = settOppUdeltSamtalereferatUtenMelding(AktivitetTypeDTO.MOTE)
        oversiktenService.lagreStartMeldingOmUdeltSamtalereferatIUtboks(happyBruker.aktorId, aktivitet.id.toLong())
        oversiktenService.sendUsendteMeldingerTilOversikten()

        val resultat = oversiktenMeldingMedMetadataDAO.hentUdelteSamtalereferatDerViIkkeHarSendtMeldingTilOversikten()

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `UdeltSamtalereferatForSamtalereferat SendtStartMelding ReturnererIngen`() {
        val (happyBruker, aktivitet) = settOppUdeltSamtalereferatUtenMelding(AktivitetTypeDTO.SAMTALEREFERAT)
        oversiktenService.lagreStartMeldingOmUdeltSamtalereferatIUtboks(happyBruker.aktorId, aktivitet.id.toLong())
        oversiktenService.sendUsendteMeldingerTilOversikten()

        val resultat = oversiktenMeldingMedMetadataDAO.hentUdelteSamtalereferatDerViIkkeHarSendtMeldingTilOversikten()

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `DeltSamtalereferatForMøte IkkeSendtMelding ReturnererIngen`() {
        settOppDeltSamtalereferatUtenMelding(AktivitetTypeDTO.MOTE)

        val resultat = oversiktenMeldingMedMetadataDAO.hentUdelteSamtalereferatDerViIkkeHarSendtMeldingTilOversikten()

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `DeltSamtalereferatForSamtalereferat IkkeSendtMelding ReturnererIngen`() {
        settOppDeltSamtalereferatUtenMelding(AktivitetTypeDTO.SAMTALEREFERAT)

        val resultat = oversiktenMeldingMedMetadataDAO.hentUdelteSamtalereferatDerViIkkeHarSendtMeldingTilOversikten()

        assertThat(resultat).isEmpty()
    }

    private fun settOppUdeltSamtalereferatUtenMelding(aktivitetType: AktivitetTypeDTO): Pair<MockBruker, AktivitetDTO> {
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(happyBruker)
        val aktivitet = aktivitetTestService.opprettAktivitet(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(aktivitetType).setErReferatPublisert(false)
                .setReferat("Et referat")
        )

        jdbcTemplate.execute("TRUNCATE OVERSIKTEN_MELDING_AKTIVITET_MAPPING")
        jdbcTemplate.execute("TRUNCATE OVERSIKTEN_MELDING_MED_METADATA")

        return Pair(happyBruker, aktivitet)
    }

    private fun settOppDeltSamtalereferatUtenMelding(aktivitetType: AktivitetTypeDTO): Pair<MockBruker, AktivitetDTO> {
        val happyBruker = navMockService.createBruker()
        val veileder = navMockService.createVeileder(happyBruker)
        val aktivitet = aktivitetTestService.opprettAktivitet(
            happyBruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(aktivitetType).setErReferatPublisert(true)
                .setReferat("Et referat")
        )

        jdbcTemplate.execute("TRUNCATE OVERSIKTEN_MELDING_AKTIVITET_MAPPING")
        jdbcTemplate.execute("TRUNCATE OVERSIKTEN_MELDING_MED_METADATA")

        return Pair(happyBruker, aktivitet)
    }
}