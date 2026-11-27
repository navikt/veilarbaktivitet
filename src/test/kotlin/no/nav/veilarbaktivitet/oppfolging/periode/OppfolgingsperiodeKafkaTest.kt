package no.nav.veilarbaktivitet.oppfolging.periode

import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.oversikten.OversiktenMelding
import no.nav.veilarbaktivitet.oversikten.OversiktenMeldingMedMetadataDAO
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import no.nav.veilarbaktivitet.util.KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION
import org.apache.kafka.clients.consumer.Consumer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

internal class OppfolgingsperiodeKafkaTest : SpringBootTestBase() {

    @Autowired
    lateinit var producer: KafkaTemplate<String, String>

    @Value("\${topic.inn.oppfolgingsperiode}")
    lateinit var oppfolgingsperiodeTopic: String

    @Value("\${topic.ut.siste-aktivitet-hendelse}")
    lateinit var sisteAktivitetHendelseTopic: String

    @Autowired
    lateinit var sistePeriodeDAO: SistePeriodeDAO

    @Autowired
    lateinit var oppfolgingsperiodeDAO: OppfolgingsperiodeDAO

    @Autowired
    lateinit var oversiktenMeldingMedMetadataDAO: OversiktenMeldingMedMetadataDAO

    private lateinit var sisteAktivitetConsumer: Consumer<String, String>

    @BeforeEach
    fun beforeEach() {
        sisteAktivitetConsumer = kafkaTestService.createStringStringConsumer(sisteAktivitetHendelseTopic)
    }

    @AfterEach
    fun afterEach() {
        sisteAktivitetConsumer.close()
    }

    @Test
    fun `start på oppfølgingsperiode skal lagre åpen periode`() {
        val bruker = brukerUtenOppfolging()
        val periode = startPeriode(bruker)

        sendOppfolgingsperiode(periode)

        val lagret = oppfolgingsperiodeDAO.getOppfolgingsperiode(periode.uuid)!!
        assertThat(lagret.aktorid).isEqualTo(bruker.aktorId.get())
        assertThat(lagret.startTid).isEqualTo(periode.startDato)
        assertThat(lagret.sluttTid).isNull()
        assertThat(oppfolgingsperiodeService.hentNåværendeÅpenPeriode(bruker.aktorId)?.oppfolgingsperiodeId)
            .isEqualTo(periode.uuid)
        val sistePeriode = sistePeriodeDAO.hentSisteOppfolgingsPeriode(bruker.aktorId).orElseThrow()
        assertThat(sistePeriode.oppfolgingsperiodeId).isEqualTo(periode.uuid)
        assertThat(sistePeriode.sluttTid).isNull()
    }

    @Test
    fun `slutt på oppfølgingsperiode skal avslutte eksisterende periode`() {
        val bruker = brukerUtenOppfolging()
        val start = startPeriode(bruker)
        sendOppfolgingsperiode(start)
        val slutt = start.withSluttDato(nå())

        sendOppfolgingsperiode(slutt)

        val lagret = oppfolgingsperiodeDAO.getOppfolgingsperiode(start.uuid)!!
        assertThat(lagret.startTid).isEqualTo(start.startDato)
        assertThat(lagret.sluttTid).isEqualTo(slutt.sluttDato)
        assertThat(oppfolgingsperiodeService.hentNåværendeÅpenPeriode(bruker.aktorId)).isNull()
        assertThat(sistePeriodeDAO.hentSisteOppfolgingsPeriode(bruker.aktorId).orElseThrow().sluttTid)
            .isEqualTo(slutt.sluttDato)
    }

    @Test
    fun `slutt på oppfølgingsperiode uten forutgående start skal lagre avsluttet periode`() {
        val bruker = brukerUtenOppfolging()
        val avsluttet = startPeriode(bruker).withSluttDato(nå())

        sendOppfolgingsperiode(avsluttet)

        val lagret = oppfolgingsperiodeDAO.getOppfolgingsperiode(avsluttet.uuid)!!
        assertThat(lagret.startTid).isEqualTo(avsluttet.startDato)
        assertThat(lagret.sluttTid).isEqualTo(avsluttet.sluttDato)
    }

    @Test
    fun `ny periode etter avsluttet periode skal bli gjeldende åpen periode`() {
        val bruker = brukerUtenOppfolging()
        val førstePeriode = startPeriode(bruker, ZonedDateTime.now().minusDays(10))
        sendOppfolgingsperiode(førstePeriode)
        sendOppfolgingsperiode(førstePeriode.withSluttDato(nå().minusDays(5)))
        val andrePeriode = startPeriode(bruker, ZonedDateTime.now().minusDays(1))

        sendOppfolgingsperiode(andrePeriode)

        val perioder = oppfolgingsperiodeDAO.getByAktorId(bruker.aktorId)
        assertThat(perioder.map { it.oppfolgingsperiodeId })
            .containsExactly(andrePeriode.uuid, førstePeriode.uuid)
        assertThat(oppfolgingsperiodeService.hentNåværendeÅpenPeriode(bruker.aktorId)?.oppfolgingsperiodeId)
            .isEqualTo(andrePeriode.uuid)
        assertThat(sistePeriodeDAO.hentSisteOppfolgingsPeriode(bruker.aktorId).orElseThrow().oppfolgingsperiodeId)
            .isEqualTo(andrePeriode.uuid)
    }

    @Test
    fun `slutt på oppfølgingsperiode skal sette aktiviteter til historiske`() {
        val bruker = navMockService.createHappyBruker()
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(
            bruker,
            AktivitetDTOMapper.mapTilAktivitetDTO(AktivitetDataTestBuilder.nyEgenaktivitet(), false)
        )

        sendOppfolgingsperiode(avsluttGjeldendePeriode(bruker))

        val historisk = aktivitetTestService.hentAktiviteterForFnr(bruker).aktiviteter
            .first { it.id == aktivitet.id }
        assertThat(historisk.isHistorisk).isTrue()
        assertThat(historisk.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.BLE_HISTORISK)
    }

    @Test
    fun `slutt på oppfølgingsperiode skal sende tombstone for siste-aktivitet på bruker`() {
        val bruker = navMockService.createHappyBruker()

        sendOppfolgingsperiode(avsluttGjeldendePeriode(bruker))

        val tombstone = KafkaTestUtils.getSingleRecord(
            sisteAktivitetConsumer,
            sisteAktivitetHendelseTopic,
            DEFAULT_WAIT_TIMEOUT_DURATION
        )
        assertThat(tombstone.key()).isEqualTo(bruker.aktorId.get())
        assertThat(tombstone.value()).isNull()
    }

    @Test
    fun `start på oppfølgingsperiode skal ikke sende tombstone for siste-aktivitet`() {
        val bruker = brukerUtenOppfolging()

        sendOppfolgingsperiode(startPeriode(bruker))

        val meldinger = KafkaTestUtils.getRecords(sisteAktivitetConsumer, Duration.ofSeconds(1))
            .records(sisteAktivitetHendelseTopic)
            .filter { it.key() == bruker.aktorId.get() }
        assertThat(meldinger).isEmpty()
    }

    @Test
    fun `slutt på oppfølgingsperiode skal lagre STOPP-melding til oversikten for udelte samtalereferat`() {
        val bruker = navMockService.createHappyBruker()
        val veileder = navMockService.createVeileder(bruker)
        aktivitetTestService.opprettAktivitetViaHttp(
            bruker,
            veileder,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.SAMTALEREFERAT)
                .setErReferatPublisert(false)
                .setOppfolgingsperiodeId(bruker.oppfolgingsperiodeId)
        )
        val startMelding = oversiktenMeldingMedMetadataDAO.hentAlleSomSkalSendes().single()
        assertThat(startMelding.operasjon).isEqualTo(OversiktenMelding.Operasjon.START)
        oversiktenMeldingMedMetadataDAO.markerSomSendt(startMelding.id)

        sendOppfolgingsperiode(avsluttGjeldendePeriode(bruker))

        val stoppMelding = oversiktenMeldingMedMetadataDAO.hentAlleSomSkalSendes().single()
        assertThat(stoppMelding.operasjon).isEqualTo(OversiktenMelding.Operasjon.STOPP)
        assertThat(stoppMelding.meldingKey).isEqualTo(startMelding.meldingKey)
        assertThat(stoppMelding.fnr.get()).isEqualTo(bruker.fnr)
        assertThat(stoppMelding.kategori).isEqualTo(OversiktenMelding.Kategori.UDELT_SAMTALEREFERAT)
    }

    private fun sendOppfolgingsperiode(periode: SisteOppfolgingsperiodeV1) {
        val sendResult = producer.send(oppfolgingsperiodeTopic, periode.aktorId, JsonUtils.toJson(periode)).get()
        kafkaTestService.assertErKonsumertNavCommon(oppfolgingsperiodeTopic, sendResult.recordMetadata.offset())
    }

    private fun brukerUtenOppfolging(): MockBruker =
        navMockService.createBruker(BrukerOptions.happyBruker().toBuilder().underOppfolging(false).build())

    private fun startPeriode(bruker: MockBruker, start: ZonedDateTime = ZonedDateTime.now().minusHours(1)) =
        SisteOppfolgingsperiodeV1.builder()
            .uuid(UUID.randomUUID())
            .aktorId(bruker.aktorId.get())
            .startDato(start.truncatedTo(ChronoUnit.MILLIS))
            .build()

    private fun avsluttGjeldendePeriode(bruker: MockBruker): SisteOppfolgingsperiodeV1 {
        val gjeldende = oppfolgingsperiodeService.hentNåværendeÅpenPeriode(bruker.aktorId)!!
        return SisteOppfolgingsperiodeV1.builder()
            .uuid(gjeldende.oppfolgingsperiodeId)
            .aktorId(bruker.aktorId.get())
            .startDato(gjeldende.startTid)
            .sluttDato(nå())
            .build()
    }

    private fun nå() = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS)
}
