package no.nav.veilarbaktivitet.oversikten

import no.nav.common.types.identer.Fnr
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.temporal.ChronoUnit
import java.util.*

open class OversiktenServiceTest: SpringBootTestBase() {

    @MockitoBean
    private lateinit var oversiktenProducer: OversiktenProducer
    
    private val bruker = navMockService.createHappyBruker()
    
    @Autowired
    private lateinit var oversiktenMeldingMedMetadataDAO: OversiktenMeldingMedMetadataDAO

    @Autowired
    private lateinit var oversiktenService: OversiktenService

    @BeforeEach
    fun beforeEach() {
        jdbcTemplate.execute("TRUNCATE TABLE oversikten_melding_med_metadata")
    }

    @Test
    fun `Skal sende usendte meldinger`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.SKAL_SENDES)
        val sendtMelding = melding(bruker, utsendingStatus = UtsendingStatus.SENDT)
        oversiktenMeldingMedMetadataDAO.lagre(melding)
        oversiktenMeldingMedMetadataDAO.lagre(sendtMelding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        verify(oversiktenProducer, times(1))
            .sendMelding(melding.meldingKey.toString(), melding.meldingSomJson)
        verifyNoMoreInteractions(oversiktenProducer)
    }

    @Test
    fun `Skal ikke sende melding som er markert som SENDT`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.SENDT)
        oversiktenMeldingMedMetadataDAO.lagre(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        verifyNoInteractions(oversiktenProducer)
    }

    @Test
    fun `Skal ikke sende melding som er markert som SKAL_IKKE_SENDES`() {
        val melding = melding(bruker, utsendingStatus = UtsendingStatus.SKAL_IKKE_SENDES)
        oversiktenMeldingMedMetadataDAO.lagre(melding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        verifyNoInteractions(oversiktenProducer)
    }

    @Test
    fun `Nye meldinger skal ikke påvirke andre meldinger`() {
        val førsteMelding = melding(bruker, utsendingStatus = UtsendingStatus.SENDT)
        val førsteMeldingID = oversiktenMeldingMedMetadataDAO.lagre(førsteMelding)
        val andreMelding = melding(meldingKey = førsteMelding.meldingKey, bruker = bruker, utsendingStatus = UtsendingStatus.SKAL_SENDES)
        oversiktenMeldingMedMetadataDAO.lagre(andreMelding)

        oversiktenService.sendUsendteMeldingerTilOversikten()

        val førsteMeldingEtterAndreMeldingErSendt = oversiktenMeldingMedMetadataDAO.hent(førsteMeldingID)!!
        assertThat(førsteMelding.tidspunktSendt).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.tidspunktSendt)
        assertThat(førsteMelding.fnr).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.fnr)
        assertThat(førsteMelding.meldingSomJson).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.meldingSomJson)
        assertThat(førsteMelding.kategori).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.kategori)
        assertThat(førsteMelding.operasjon).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.operasjon)
        assertThat(førsteMelding.opprettet.truncatedTo(ChronoUnit.MILLIS)).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.opprettet.truncatedTo(
            ChronoUnit.MILLIS))
        assertThat(førsteMelding.utsendingStatus).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.utsendingStatus)
        assertThat(førsteMelding.meldingKey).isEqualTo(førsteMeldingEtterAndreMeldingErSendt.meldingKey)
    }

    private fun melding(bruker: MockBruker, meldingKey: UUID = UUID.randomUUID(), utsendingStatus: UtsendingStatus = UtsendingStatus.SKAL_SENDES) =
        OversiktenMeldingMedMetadata(
            fnr = Fnr.of(bruker.fnr),
            meldingSomJson = "{}",
            kategori = OversiktenMelding.Kategori.UDELT_SAMTALEREFERAT,
            meldingKey = meldingKey,
            utsendingStatus = utsendingStatus,
            operasjon = OversiktenMelding.Operasjon.START
        )
    
}