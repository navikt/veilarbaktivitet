package no.nav.veilarbaktivitet.aktivitetskort

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import no.nav.common.json.JsonUtils
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.types.identer.NavIdent
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.domain.Ident
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.aktivitet.dto.EksternAktivitetDTO
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortProducerUtil.extraFieldRecord
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortProducerUtil.invalidDateFieldRecord
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortProducerUtil.kafkaArenaAktivitetWrapper
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortProducerUtil.missingFieldRecord
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling
import no.nav.veilarbaktivitet.aktivitetskort.dto.*
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.*
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDto
import no.nav.veilarbaktivitet.aktivitetskort.service.AktivitetskortService
import no.nav.veilarbaktivitet.aktivitetskort.service.TiltakMigreringCronService
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder
import no.nav.veilarbaktivitet.mock_nav_modell.WireMockUtil
import no.nav.veilarbaktivitet.oppfolging.periode.SisteOppfolgingsperiodeV1
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.util.AktivitetskortFeilListener
import no.nav.veilarbaktivitet.util.AktivitetskortIdMappingListener
import no.nav.veilarbaktivitet.util.DateUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.net.ssl.SSLHandshakeException
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AktivitetskortConsumerIntegrationTest(
    @Autowired
    val aktivitetskortFeilListener: AktivitetskortFeilListener,
    @Autowired
    val aktivitetskortIdMappingListener: AktivitetskortIdMappingListener,
    @Value("\${topic.inn.aktivitetskort}")
    var topic: String,
    @Autowired
    var producerClient: KafkaProducerClient<String, String>,
    @Autowired
    var aktivitetskortConsumer: AktivitetskortConsumer,
    @Autowired
    var messageDAO: AktivitetsMessageDAO,
    @Autowired
    var tiltakMigreringCronService: TiltakMigreringCronService,
    @Autowired
    var meterRegistry: MeterRegistry,
    @Autowired
    var brukernotifikasjonAssertsConfig: BrukernotifikasjonAssertsConfig
) : SpringBootTestBase() {
    @Autowired
    lateinit var aktivitetskortService: AktivitetskortService

    private lateinit var brukernotifikasjonAsserts: BrukernotifikasjonAsserts
    private lateinit var mockBruker: MockBruker
    private lateinit var veileder: MockVeileder
    private val endretDato = ZonedDateTime.now()

    @BeforeAll
    fun beforeAll() {
        mockBruker = navMockService.createBruker()
        veileder = navMockService.createVeileder(mockBruker)
    }

    @BeforeEach
    fun cleanupBetweenTests() {
        Mockito.`when`(unleash.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)).thenReturn(true)
        brukernotifikasjonAsserts = BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig)
        aktivitetskortFeilListener.clearQueue()
    }

    fun aktivitetskort(
        funksjonellId: UUID,
        aktivitetStatus: AktivitetskortStatus,
        bruker: MockBruker = mockBruker
    ): Aktivitetskort {
        return AktivitetskortUtil.ny(
            funksjonellId,
            aktivitetStatus,
            endretDato,
            bruker
        )
    }

    private fun assertFeilmeldingPublished(
        funksjonellId: UUID,
        errorType: ErrorType,
        source: MessageSource
    ) {
        val singleRecord = aktivitetskortFeilListener.getSingleRecord()!!
        val payload = JsonUtils.fromJson(singleRecord.value(), AktivitetskortFeilMelding::class.java)
        assertThat(singleRecord.key()).isEqualTo(funksjonellId.toString())
        assertThat(payload.errorType).isEqualTo(errorType)
        assertThat(payload.source).isEqualTo(source)
    }

    private fun assertIdMappingPublished(funksjonellId: UUID, arenaId: ArenaId) {
//        val singleRecord = KafkaTestUtils.getSingleRecord(
//            aktivitetskortIdMappingConsumer,
//            aktivitetskortIdMappingTopic,
//            KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION
//        )

        val singleRecord = aktivitetskortIdMappingListener.getSingleRecord()!!
        val payload = JsonUtils.fromJson(singleRecord.value(), IdMappingDto::class.java)
        assertThat(singleRecord.key()).isEqualTo(funksjonellId.toString())
        assertThat(payload.arenaId).isEqualTo(arenaId)
    }

    @Test
    fun happy_case_upsert_ny_arenatiltaksaktivitet() {
        //trenges for og teste med count hvis ikke må man også matche på tags for å få testet counten
        //burde man endre på metrikkene her? kan man vite en fulstendig liste av aktiviteskort og skilde?
        meterRegistry.find(AktivitetskortMetrikker.AKTIVITETSKORT_UPSERT).meters()
            .forEach(java.util.function.Consumer { it: Meter -> meterRegistry.remove(it) })
        val actual = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)
        val arenaHeaders = arenaMeldingHeaders(mockBruker, ArenaId("ARENATA123"), "MIDL")
        aktivitetTestService.opprettEksterntArenaKort(ArenaKort(actual, arenaHeaders))
        val count = meterRegistry.find(AktivitetskortMetrikker.AKTIVITETSKORT_UPSERT).counter()?.count()
        assertThat(count).isEqualTo(1.0)
        val aktivitet = hentAktivitet(actual.id)
        assertThat(aktivitet.type).isEqualTo(AktivitetTypeDTO.EKSTERNAKTIVITET)
        assertEquals(AktivitetTransaksjonsType.OPPRETTET, aktivitet.transaksjonsType)
        assertEquals(AktivitetStatus.PLANLAGT, aktivitet.status)
        assertTrue(aktivitet.isAvtalt)
        assertEquals(Innsender.ARENAIDENT.name, aktivitet.endretAvType)
        assertEquals(
            aktivitet.eksternAktivitet, EksternAktivitetDTO(
                AktivitetskortType.ARENA_TILTAK,
                null, emptyList(),
                actual.detaljer,
                actual.etiketter
            )
        )
        assertIdMappingPublished(actual.id, arenaHeaders.eksternReferanseId)
    }

    @Test
    fun aktiviteter_opprettet_av_bruker_skal_ha_riktig_endretAv_verdi() {
        val brukerIdent = "12129312122"
        val aktivitetskort = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)
            .copy(
                endretAv = Ident(
                    brukerIdent,
                    IdentType.PERSONBRUKERIDENT
                )
            )
        val kafkaAktivitetskortWrapperDTO = KafkaAktivitetskortWrapperDTO(
            aktivitetskort, UUID.randomUUID(), AktivitetskortType.ARBEIDSTRENING, MessageSource.TEAM_TILTAK
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(kafkaAktivitetskortWrapperDTO))
        val resultat = hentAktivitet(aktivitetskort.id)
        assertThat(resultat.endretAv).isEqualTo(brukerIdent)
        assertThat(resultat.endretAvType).isEqualTo(Innsender.BRUKER.toString())
    }

    @Test
    fun ekstern_aktivitet_skal_ha_oppfolgingsperiode() {
        val funksjonellId = UUID.randomUUID()
        val actual = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
        val wrapperDTO = KafkaAktivitetskortWrapperDTO(
            aktivitetskortType = AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            aktivitetskort = actual,
            source = "source",
            messageId = UUID.randomUUID()
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(wrapperDTO))
        val aktivitet = hentAktivitet(funksjonellId)
        assertEquals(mockBruker.oppfolgingsperiodeId, aktivitet.oppfolgingsperiodeId)
    }

    @Test
    fun rekrutteringstreff_fra_rekrutteringsbistand_skal_behandles() {
        val funksjonellId = UUID.randomUUID()
        val actual = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
        val wrapperDTO = KafkaAktivitetskortWrapperDTO(
            aktivitetskortType = AktivitetskortType.REKRUTTERINGSTREFF,
            aktivitetskort = actual,
            source = MessageSource.REKRUTTERINGSBISTAND.name,
            messageId = UUID.randomUUID()
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(wrapperDTO))
        val aktivitet = hentAktivitet(funksjonellId)
        assertEquals(AktivitetTypeDTO.EKSTERNAKTIVITET, aktivitet.type)
        assertEquals(AktivitetskortType.REKRUTTERINGSTREFF, aktivitet.eksternAktivitet.type)
    }

    @Test
    fun lonnstilskudd_på_bruker_som_ikke_er_under_oppfolging_skal_feile() {
        val brukerUtenOppfolging =
            navMockService.createBruker(BrukerOptions.happyBruker().toBuilder().underOppfolging(false).build())
        val serie = AktivitetskortSerie(brukerUtenOppfolging)
        val actual =
            serie.ny(AktivitetskortStatus.PLANLAGT, ZonedDateTime.now()).copy(source = MessageSource.TEAM_TILTAK.name)
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(actual))
        assertFeilmeldingPublished(
            serie.funksjonellId,
            ErrorType.MANGLER_OPPFOLGINGSPERIODE,
            MessageSource.TEAM_TILTAK
        )
    }

    @Test
    fun historisk_arenatiltak_aktivitet_skal_ha_oppfolgingsperiode() {
        val gammelperiode = SerieOppfolgingsperiode(UUID.randomUUID(), ZonedDateTime.now().minusDays(1))
        val serie = ArenaAktivitetskortSerie(mockBruker, "MIDLONNTIL", gammelperiode)
        val actual = serie.ny(AktivitetskortStatus.PLANLAGT, endretTidspunkt = ZonedDateTime.now().minusDays(75))
        aktivitetTestService.opprettEksterntArenaKort(actual)
        aktivitetTestService.upsertOppfolgingsperiode(SisteOppfolgingsperiodeV1.builder()
            .uuid(gammelperiode.id)
            .aktorId(mockBruker.aktorId.get())
            .startDato(gammelperiode.slutt?.minusDays(10))
            .sluttDato(gammelperiode.slutt).build()
        )
        val aktivitetFoer = hentAktivitet(serie.funksjonellId)
        assertThat(aktivitetFoer.oppfolgingsperiodeId).isNotNull()
        assertThat(aktivitetFoer.isHistorisk).isFalse()
        assertThat(aktivitetFoer.eksternAktivitet.detaljer).isNotEmpty
        val aktivitetFoerOpprettetSomHistorisk = jdbcTemplate.queryForObject(
            """
                SELECT opprettet_som_historisk
                FROM EKSTERNAKTIVITET
                WHERE AKTIVITET_ID = ?
                ORDER BY VERSJON desc
                LIMIT 1
                """.trimIndent(), Boolean::class.javaPrimitiveType!!, aktivitetFoer.id.toLong()
        )
        assertThat(aktivitetFoerOpprettetSomHistorisk).isTrue()
        tiltakMigreringCronService!!.settTiltakOpprettetSomHistoriskTilHistorisk()
        val aktivitetEtter = hentAktivitet(serie.funksjonellId)
        assertThat(aktivitetEtter.isHistorisk).isTrue()
        assertThat(aktivitetEtter.eksternAktivitet.detaljer).isNotEmpty
    }

    @Test
    fun `historisk aktivitet kan endres av ACL`() {
        val avsluttetPeriode = SerieOppfolgingsperiode(UUID.randomUUID(), ZonedDateTime.now().minusDays(1))
        val serie = ArenaAktivitetskortSerie(mockBruker, "VARIG", avsluttetPeriode)
        val opprettMelding: ArenaKort =
            serie.ny(AktivitetskortStatus.GJENNOMFORES, avsluttetPeriode.slutt!!.minusDays(2))
        val endretMelding: ArenaKort = serie.ny(AktivitetskortStatus.AVBRUTT, avsluttetPeriode.slutt.minusDays(1))
        aktivitetTestService.upsertOppfolgingsperiode(SisteOppfolgingsperiodeV1.builder()
            .uuid(avsluttetPeriode.id)
            .aktorId(mockBruker.aktorId.get())
            .startDato(avsluttetPeriode.slutt.minusDays(10))
            .sluttDato(avsluttetPeriode.slutt).build()
        )
        aktivitetTestService.opprettEksterntArenaKort(opprettMelding)
        tiltakMigreringCronService.settTiltakOpprettetSomHistoriskTilHistorisk()
        aktivitetTestService.opprettEksterntArenaKort(endretMelding)
        val aktivitet = hentAktivitet(serie.funksjonellId)
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.AVBRUTT)
    }

    @Test
    fun `historisk aktivitet kan ikke endres av Komet`() {
        val avsluttetPeriode = SerieOppfolgingsperiode(UUID.randomUUID(), ZonedDateTime.now().minusDays(1))
        val serie = ArenaAktivitetskortSerie(mockBruker, "AVKLARAG", avsluttetPeriode)
        val opprettMelding: ArenaKort =
            serie.ny(AktivitetskortStatus.GJENNOMFORES, avsluttetPeriode.slutt!!.minusDays(2))
        val endretMelding: ArenaKort = serie.ny(AktivitetskortStatus.AVBRUTT, avsluttetPeriode.slutt!!.minusDays(1))
        aktivitetTestService.opprettEksterntArenaKort(opprettMelding)
        tiltakMigreringCronService!!.settTiltakOpprettetSomHistoriskTilHistorisk()
        aktivitetTestService.opprettEksterntAktivitetsKort(
            listOf(
                endretMelding.melding.copy(
                    source = MessageSource.TEAM_KOMET.name,
                    aktivitetskortType = AktivitetskortType.AVKLARAG
                )
            )
        )
        assertFeilmeldingPublished(
            serie.funksjonellId,
            ErrorType.ULOVLIG_ENDRING,
            MessageSource.TEAM_KOMET
        )
    }


    @Test
    fun happy_case_upsert_status_existing_tiltaksaktivitet() {
        val serie = AktivitetskortSerie(mockBruker)
        val tiltaksaktivitet = serie.ny(AktivitetskortStatus.PLANLAGT, ZonedDateTime.now().minusHours(2))
        val annenVeileder = Ident("ANNEN_NAV_IDENT", Innsender.ARENAIDENT)
        val tiltaksaktivitetUpdate = serie.ny(AktivitetskortStatus.GJENNOMFORES, ZonedDateTime.now().minusHours(1))
            .let { it.copy(aktivitetskort = it.aktivitetskort.copy(endretAv = annenVeileder)) }
        aktivitetTestService.opprettEksterntAktivitetsKort(
            listOf(
                tiltaksaktivitet,
                tiltaksaktivitetUpdate
            )
        )
        val aktivitet = hentAktivitet(serie.funksjonellId)
        assertThat(aktivitet.type).isEqualTo(AktivitetTypeDTO.EKSTERNAKTIVITET)
        assertNotNull(aktivitet)
        assertThat(aktivitet.endretDato)
            .isCloseTo(DateUtils.zonedDateTimeToDate(ZonedDateTime.now()), 1000)
        assertThat(aktivitet.endretAv).isEqualTo(annenVeileder.ident)
        assertEquals(AktivitetStatus.GJENNOMFORES, aktivitet.status)
        assertEquals(
            AktivitetTransaksjonsType.STATUS_ENDRET,
            aktivitet.transaksjonsType
        )
    }

    @Test
    fun oppdater_tiltaksaktivitet_fra_avtalt_til_ikke_avtalt_skal_throwe() {
        val funksjonellId = UUID.randomUUID()
        val lonnstilskuddAktivitet = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
            .copy(avtaltMedNav = true)
        val melding1 = KafkaAktivitetskortWrapperDTO(
            lonnstilskuddAktivitet,
            UUID.randomUUID(),
            AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            MessageSource.TEAM_TILTAK
        )
        val lonnstilskuddAktivitetUpdate: Aktivitetskort =
            aktivitetskort(funksjonellId, AktivitetskortStatus.GJENNOMFORES)
                .copy(avtaltMedNav = false)
        val melding2 = KafkaAktivitetskortWrapperDTO(
            lonnstilskuddAktivitetUpdate,
            UUID.randomUUID(),
            AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            MessageSource.TEAM_TILTAK
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(melding1, melding2))
        val aktivitet = hentAktivitet(funksjonellId)
        assertNotNull(aktivitet)
        assertFeilmeldingPublished(
            funksjonellId,
            ErrorType.ULOVLIG_ENDRING,
            MessageSource.TEAM_TILTAK
        )
    }

    @Test
    fun oppdater_tiltaksaktivitet_endre_bruker_skal_throwe() {
        val funksjonellId = UUID.randomUUID()
        val lonnstilskuddAktivitet = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
        val melding1 = KafkaAktivitetskortWrapperDTO(
            lonnstilskuddAktivitet,
            UUID.randomUUID(),
            AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            MessageSource.TEAM_TILTAK
        )
        val mockBruker2 = navMockService.createHappyBruker()
        val lonnstilskuddAktivitetUpdate: Aktivitetskort =
            aktivitetskort(funksjonellId, AktivitetskortStatus.GJENNOMFORES)
                .copy(personIdent = mockBruker2.fnr)
        val melding2 = KafkaAktivitetskortWrapperDTO(
            lonnstilskuddAktivitetUpdate,
            UUID.randomUUID(),
            AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            MessageSource.TEAM_TILTAK
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(melding1, melding2))
        val aktivitet = hentAktivitet(funksjonellId)
        assertNotNull(aktivitet)
        assertFeilmeldingPublished(
            funksjonellId,
            ErrorType.ULOVLIG_ENDRING,
            MessageSource.TEAM_TILTAK
        )
    }

    @Test
    fun duplikat_melding_bare_1_opprettet_transaksjon() {
        // Given- 1 aktivitet
        val aktivitetskort = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)
        val kafkaAktivitetskortWrapperDTO = KafkaAktivitetskortWrapperDTO(
            aktivitetskort,
            AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            MessageSource.TEAM_TILTAK
        )
        val context = arenaMeldingHeaders(mockBruker)
        // When - Sendt 2 ganger
        val producerRecord =
            aktivitetTestService.makeAktivitetskortProducerRecord(
                kafkaAktivitetskortWrapperDTO,
                context
            ) as ProducerRecord<String, String>
        producerClient!!.sendSync(producerRecord)
        val recordMetadataDuplicate = producerClient!!.sendSync(producerRecord)
        kafkaTestService.assertErKonsumert(
            topic,
            NavCommonKafkaConfig.CONSUMER_GROUP_ID,
            recordMetadataDuplicate.offset()
        )
        // Then - Skal bare finnes 1 transaksjon
        val aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter.stream()
            .filter { a: AktivitetDTO -> a.funksjonellId == aktivitetskort.id }
            .toList()
        assertEquals(1, aktiviteter.size)
        assertEquals(
            AktivitetTransaksjonsType.OPPRETTET,
            aktiviteter.stream().findFirst().get().transaksjonsType
        )
    }

    @Test
    fun oppdatering_av_detaljer_gir_riktig_transaksjon() {
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = KafkaAktivitetskortWrapperDTO(
            aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT),
            AktivitetskortType.VARIG_TILRETTELAGT_ARBEID_I_ORDINAER_VIRKSOMHET,
            MessageSource.TEAM_TILTAK
        )
        val tiltaksaktivitetEndret = KafkaAktivitetskortWrapperDTO(
            aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
                .copy(
                    detaljer = listOf(
                        Attributt(
                            "Tiltaksnavn",
                            "Nytt navn"
                        )
                    )
                ), AktivitetskortType.VARIG_TILRETTELAGT_ARBEID_I_ORDINAER_VIRKSOMHET, MessageSource.TEAM_TILTAK
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(
            listOf(
                tiltaksaktivitet,
                tiltaksaktivitetEndret
            )
        )
        val aktivitet = hentAktivitet(funksjonellId)
        assertEquals(
            AktivitetTransaksjonsType.DETALJER_ENDRET,
            aktivitet.transaksjonsType
        )
    }

    @Test
    fun skal_handtere_ukjent_source() {
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = KafkaAktivitetskortWrapperDTO(
            aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT),
            AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            MessageSource.TEAM_TILTAK
        ).copy(source = "UKJENT_SOURCE")
        aktivitetTestService.opprettEksterntAktivitetsKort(
            listOf(
                tiltaksaktivitet
            )
        )
        val aktivitet = hentAktivitet(funksjonellId)
        assertEquals(
            AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            aktivitet.eksternAktivitet.type
        )
    }

    @Test
    fun oppdatering_status_og_detaljer_gir_4_transaksjoner() {
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet: KafkaAktivitetskortWrapperDTO = KafkaAktivitetskortWrapperDTO(
            aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
                .copy(avtaltMedNav = false), AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD, MessageSource.TEAM_TILTAK
        )
        val etikett = Etikett("Fått plass", Sentiment.POSITIVE, "FATT_PLASS")
        val tiltaksaktivitetEndret = KafkaAktivitetskortWrapperDTO(
            aktivitetskort(funksjonellId, AktivitetskortStatus.GJENNOMFORES)
                .copy(avtaltMedNav = true, etiketter = listOf(etikett)),
            AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            MessageSource.TEAM_TILTAK
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(
            listOf(
                tiltaksaktivitet,
                tiltaksaktivitetEndret
            )
        )
        val aktivitetId = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter.stream()
            .findFirst().get().id
        val aktivitetVersjoner = aktivitetTestService.hentVersjoner(aktivitetId, mockBruker, mockBruker)
        assertEquals(4, aktivitetVersjoner.size)
        val sisteVersjon = aktivitetVersjoner[0]
        assertThat(sisteVersjon.isAvtalt).isTrue()
        assertThat(sisteVersjon.eksternAktivitet.etiketter).isEqualTo(tiltaksaktivitetEndret.aktivitetskort.etiketter)
        assertThat(sisteVersjon.status).isEqualTo(AktivitetStatus.GJENNOMFORES)
        assertEquals(
            AktivitetTransaksjonsType.STATUS_ENDRET,
            aktivitetVersjoner[0].transaksjonsType
        )
        assertEquals(
            AktivitetTransaksjonsType.DETALJER_ENDRET,
            aktivitetVersjoner[1].transaksjonsType
        )
        assertEquals(
            AktivitetTransaksjonsType.AVTALT,
            aktivitetVersjoner[2].transaksjonsType
        )
        assertEquals(
            AktivitetTransaksjonsType.OPPRETTET,
            aktivitetVersjoner[3].transaksjonsType
        )
    }

    @Test
    fun endretTidspunkt_skal_settes_fra_melding() {
        val context = arenaMeldingHeaders(mockBruker)
        val aktivitetskort: Aktivitetskort = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)
            .copy(endretTidspunkt = ZonedDateTime.now().minusDays(1))
        aktivitetTestService.opprettEksterntArenaKort(ArenaKort(aktivitetskort, context))
        val aktivitet = hentAktivitet(aktivitetskort.id)
        assertThat(aktivitet.endretDato).isCloseTo(DateUtils.zonedDateTimeToDate(ZonedDateTime.now()), 1000)
    }


    @Test
    fun skal_skippe_gamle_meldinger_etter_ny_melding() {
        val funksjonellId = UUID.randomUUID()
        val nyesteNavn = "Nytt navn"
        val context = arenaMeldingHeaders(mockBruker)
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
            .copy(
                detaljer = listOf(
                    Attributt(
                        "Tiltaksnavn",
                        "Gammelt navn"
                    )
                )
            )
        val tiltaksMelding = KafkaAktivitetskortWrapperDTO(
            tiltaksaktivitet,
            AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            MessageSource.TEAM_TILTAK
        )
        val tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
            .copy(
                detaljer = listOf(
                    Attributt(
                        "Tiltaksnavn",
                        nyesteNavn
                    )
                )
            )
        val tiltaksMeldingEndret = KafkaAktivitetskortWrapperDTO(
            tiltaksaktivitetEndret,
            AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            MessageSource.TEAM_TILTAK
        )
        aktivitetTestService.opprettEksterntArenaKort(
            listOf(
                ArenaKort(tiltaksMelding, context),
                ArenaKort(tiltaksMeldingEndret, context),
                ArenaKort(tiltaksMelding, context)
            )
        )
        val aktivitet = hentAktivitet(funksjonellId)
        val detaljer = aktivitet.eksternAktivitet.detaljer
        assertThat(detaljer.stream().filter { it: Attributt -> it.label == "Tiltaksnavn" }).hasSize(1)
        assertThat(detaljer).containsOnlyOnceElementsOf(
            listOf(
                Attributt(
                    "Tiltaksnavn",
                    nyesteNavn
                )
            )
        )
    }

    private fun hentAktivitet(funksjonellId: UUID): AktivitetDTO {
        return aktivitetTestService.hentAktiviteterForFnr(mockBruker, veileder)
            .aktiviteter
            .first { a: AktivitetDTO -> a.funksjonellId == funksjonellId }
    }

    @Test
    fun avbrutt_aktivitet_kan_endres() {
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetskortStatus.AVBRUTT)
        val tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
        val context = arenaMeldingHeaders(mockBruker)
        aktivitetTestService.opprettEksterntArenaKort(
            listOf(
                ArenaKort(tiltaksaktivitet, context),
                ArenaKort(tiltaksaktivitetEndret, context)
            )
        )
        val aktivitet = hentAktivitet(funksjonellId)
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
    }

    @Test
    fun fullfort_aktivitet_kan_endres() {
        val tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.FULLFORT)
        val tiltaksaktivitetEndret = aktivitetskort(tiltaksaktivitet.id, AktivitetskortStatus.PLANLAGT)
        aktivitetTestService.opprettEksterntArenaKort(
            listOf(
                ArenaKort(tiltaksaktivitet, arenaMeldingHeaders(mockBruker)),
                ArenaKort(tiltaksaktivitetEndret, arenaMeldingHeaders(mockBruker))
            )
        )
        val aktivitet = hentAktivitet(tiltaksaktivitet.id)
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
    }

    @Test
    fun aktivitet_kan_settes_til_avbrutt() {
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
        val tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetskortStatus.AVBRUTT)
        aktivitetTestService.opprettEksterntArenaKort(
            listOf(
                ArenaKort(tiltaksaktivitet, arenaMeldingHeaders(mockBruker)),
                ArenaKort(tiltaksaktivitetEndret, arenaMeldingHeaders(mockBruker))
            )
        )
        val aktivitet = hentAktivitet(funksjonellId)
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.AVBRUTT)
    }

    @Test
    fun invalid_messages_should_catch_deserializer_error() {
        val messages = listOf(
            missingFieldRecord(mockBruker.fnrAsFnr),
            extraFieldRecord(mockBruker.fnrAsFnr),
            invalidDateFieldRecord(mockBruker.fnrAsFnr)
        )
        val headers: Iterable<Header> = listOf<Header>(
            RecordHeader(
                AktivitetsbestillingCreator.HEADER_EKSTERN_REFERANSE_ID,
                ArenaId("ARENATA123").id().toByteArray()
            ),
            RecordHeader(AktivitetsbestillingCreator.HEADER_EKSTERN_ARENA_TILTAKSKODE, "MIDLONS".toByteArray())
        )
        val lastRecordMetadata = messages
            .map { (json, messageId) ->
                ProducerRecord(
                    topic,
                    null,
                    messageId.toString(),
                    json,
                    headers
                )
            }
            .map { record: ProducerRecord<String, String>? -> producerClient!!.sendSync(record) }
            .reduce { first: RecordMetadata?, second: RecordMetadata -> second }
        kafkaTestService.assertErKonsumert(topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, lastRecordMetadata.offset())
        val records = aktivitetskortFeilListener.take(messages.size)
        assertThat(records.count()).isEqualTo(messages.size)
    }

    @Test
    fun should_catch_ugyldigident_error() {
        val brukerUtenOppfolging =
            navMockService.createBruker(BrukerOptions.happyBruker().toBuilder().underOppfolging(false).build())
        WireMockUtil.aktorUtenGjeldende(brukerUtenOppfolging.fnr, brukerUtenOppfolging.aktorId)
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet =
            aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT, bruker = brukerUtenOppfolging)
        aktivitetTestService.opprettEksterntAktivitetsKort(
            listOf(
                KafkaAktivitetskortWrapperDTO(
                    aktivitetskort = tiltaksaktivitet,
                    messageId = UUID.randomUUID(),
                    aktivitetskortType = AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
                    source = MessageSource.TEAM_TILTAK.name
                )
            )
        )
        assertFeilmeldingPublished(
            funksjonellId,
            ErrorType.UGYLDIG_IDENT,
            MessageSource.TEAM_TILTAK
        )
    }

    @Test
    fun should_throw_runtime_exception_on_missing_arena_headers() {
        var aktivitetWrapper = kafkaArenaAktivitetWrapper(mockBruker.fnrAsFnr)
        aktivitetWrapper = aktivitetWrapper.copy(aktivitetskortType = AktivitetskortType.ARENA_TILTAK)
        val h1 = RecordHeader("NONSENSE_HEADER", "DUMMYVALUE".toByteArray())
        val record = ConsumerRecord(
            topic,
            0,
            0,
            aktivitetWrapper.aktivitetskort.id.toString(),
            JsonUtils.toJson(aktivitetWrapper)
        )
        record.headers().add(h1)
        /* Call consume directly to avoid retry / waiting for message to be consumed */
        val runtimeException = assertThrows(
            RuntimeException::class.java
        ) { aktivitetskortConsumer!!.consume(record) }
        assertThat(runtimeException.message).isEqualTo("java.lang.RuntimeException: Mangler Arena Header for arena-id aktivitetskort")
    }

    @Test
    fun should_throw_runtime_exception_on_missing_messageId() {
        val aktivitetWrapper = kafkaArenaAktivitetWrapper(mockBruker.fnrAsFnr)
            .copy(messageId = null, aktivitetskortType = AktivitetskortType.VARIG_LONNSTILSKUDD)
        val record = ConsumerRecord(
            topic,
            0,
            0,
            aktivitetWrapper.aktivitetskort.id.toString(),
            JsonUtils.toJson(aktivitetWrapper)
        )
        aktivitetskortConsumer!!.consume(record)
        assertFeilmeldingPublished(
            aktivitetWrapper.getAktivitetskortId(),
            ErrorType.DESERIALISERINGSFEIL,
            MessageSource.UNKNOWN
        )
    }

    @Test
    fun should_handle_messageId_in_header() {
        var aktivitetWrapper = kafkaArenaAktivitetWrapper(mockBruker.fnrAsFnr)
        aktivitetWrapper = aktivitetWrapper.copy(
            aktivitetskortType = AktivitetskortType.VARIG_LONNSTILSKUDD,
            messageId = null
        )
        val messageId = UUID.randomUUID()
        val record = ProducerRecord(
            topic,
            0,
            Date().time,
            aktivitetWrapper.aktivitetskort.id.toString(),
            JsonUtils.toJson(aktivitetWrapper)
        )
        val msgIdHeader: Header =
            RecordHeader(AktivitetskortConsumer.UNIQUE_MESSAGE_IDENTIFIER, messageId.toString().toByteArray())
        record.headers().add(msgIdHeader)
        val sendResult = aktivitetTestService.opprettEksterntAktivitetsKort(record)
        assertThat(sendResult.recordMetadata.hasOffset()).isTrue()
    }

    @Test
    fun should_throw_exception_when_messageId_is_equal_to_funksjonell_id() {
        val funksjonellId = UUID.randomUUID()
        val actual = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
        val wrapperDTO = KafkaAktivitetskortWrapperDTO(
            messageId = funksjonellId,
            aktivitetskortType = AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            aktivitetskort = actual,
            source = MessageSource.TEAM_TILTAK.name
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(wrapperDTO))
        assertFeilmeldingPublished(
            funksjonellId,
            ErrorType.MESSAGEID_LIK_AKTIVITETSID,
            MessageSource.TEAM_TILTAK
        )
    }

    @Test
    fun should_not_commit_database_transaction_if_runtimeException_is_thrown() {
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
        val tiltaksaktivitetOppdatert: Aktivitetskort = aktivitetskort(funksjonellId, AktivitetskortStatus.AVBRUTT)
            .copy(
                detaljer = listOf(
                    Attributt(
                        "Tiltaksnavn",
                        "Nytt navn"
                    )
                )
            )
        val aktivitetskort = KafkaAktivitetskortWrapperDTO(
            tiltaksaktivitet,
            AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            MessageSource.TEAM_TILTAK
        )
        val aktivitetskortOppdatert = KafkaAktivitetskortWrapperDTO(
            tiltaksaktivitetOppdatert,
            AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            MessageSource.TEAM_TILTAK
        )
        val h1 = RecordHeader(
            AktivitetsbestillingCreator.HEADER_EKSTERN_REFERANSE_ID,
            ArenaId("ARENATA123").id().toByteArray()
        )
        val h2 = RecordHeader(AktivitetsbestillingCreator.HEADER_EKSTERN_ARENA_TILTAKSKODE, "MIDLONS".toByteArray())
        val record = ConsumerRecord(topic, 0, 0, funksjonellId.toString(), JsonUtils.toJson(aktivitetskort))
        record.headers().add(h1)
        record.headers().add(h2)
        val recordOppdatert =
            ConsumerRecord(topic, 0, 1, funksjonellId.toString(), JsonUtils.toJson(aktivitetskortOppdatert))
        recordOppdatert.headers().add(h1)
        recordOppdatert.headers().add(h2)

        /* Call consume directly to avoid retry / waiting for message to be consumed */aktivitetskortConsumer!!.consume(
            record
        )

        /* Simulate technical error after DETALJER_ENDRET processing */

        /* NB får ikke testet med checked exception */
        Mockito.doThrow(IllegalArgumentException("RunTimeException"))
            .`when`(aktivitetskortService).oppdaterStatus(any(), any())
        assertThrows(RuntimeException::class.java) {
            aktivitetskortConsumer.consume(recordOppdatert)
        }

        /* Assert successful rollback */
        assertThat(messageDAO.exist(aktivitetskortOppdatert.messageId!!))
            .isFalse()
        val aktivitet = hentAktivitet(funksjonellId)
        assertThat(aktivitet.eksternAktivitet.detaljer[0].verdi).isEqualTo(
            tiltaksaktivitet.detaljer!![0].verdi
        )
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
    }

    @Test
    fun aktivitet_med_fho_for_migrering_skal_ha_fho_etter_migrering() {
        val arenaaktivitetId = ArenaId("ARENATA717171")
        val arenaAktivitetDTO =
            aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, arenaaktivitetId, veileder)
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
        aktivitetTestService.opprettEksterntArenaKort(
            ArenaKort(
                tiltaksaktivitet,
                arenaMeldingHeaders(mockBruker, arenaaktivitetId)
            )
        )

        val aktivitet = hentAktivitet(funksjonellId)
        assertNotNull(aktivitet.forhaandsorientering)
        assertThat(aktivitet.endretAv).isEqualTo(tiltaksaktivitet.endretAv.ident)
        assertThat(arenaAktivitetDTO.forhaandsorientering.id).isEqualTo(aktivitet.forhaandsorientering.id)
        assertThat(aktivitet.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.OPPRETTET)
    }

    @Test
    fun skal_migrere_eksisterende_forhaandorientering() {
        // Det finnes en arenaaktivtiet fra før
        val arenaaktivitetId = ArenaId("ARENATA123")
        // Opprett FHO på aktivitet
        aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, arenaaktivitetId, veileder)
        val varsel = brukernotifikasjonAsserts!!.assertOppgaveSendt(mockBruker.fnrAsFnr)
        // Migrer arenaaktivitet via topic
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
        aktivitetTestService.opprettEksterntArenaKort(
            ArenaKort(tiltaksaktivitet, arenaMeldingHeaders(mockBruker, arenaaktivitetId)),
        )
        // Bruker leser fho (POST på /lest)
        val aktivitet = hentAktivitet(funksjonellId)
        aktivitetTestService.lesFHO(mockBruker, aktivitet.id.toLong(), aktivitet.versjon.toLong())
        // Skal dukke opp Done melding på brukernotifikasjons-topic
        brukernotifikasjonAsserts!!.assertInaktivertMeldingErSendt(varsel.varselId)
    }

    @Test
    @Disabled("TODO Re-introduser når vi begynner på gruppe/utdanningstiltak")
    fun tiltak_endepunkt_skal_legge_pa_aktivitet_id_og_versjon_pa_migrerte_arena_aktiviteteter() {
        val arenaaktivitetId = ArenaId("ARENATA123")
        val tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)
        Mockito.`when`(unleash.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE))
            .thenReturn(false)
        val preMigreringArenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId)
        assertThat(preMigreringArenaAktiviteter).hasSize(1)
        assertThat(preMigreringArenaAktiviteter[0].id).isEqualTo(arenaaktivitetId.id()) // Skal være arenaid
        val context = arenaMeldingHeaders(mockBruker, arenaaktivitetId)
        aktivitetTestService.opprettEksterntArenaKort(ArenaKort(tiltaksaktivitet, context))
        Mockito.`when`(unleash.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)).thenReturn(true)
        val aktivitet = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter.stream().findFirst().get()
        val tekniskId = aktivitet.id
        val versjon = aktivitet.versjon.toLong()
        Mockito.`when`(unleash.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE))
            .thenReturn(false)
        val postMigreringArenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId)
        assertThat(postMigreringArenaAktiviteter).hasSize(1)
        val migrertArenaAktivitet = postMigreringArenaAktiviteter[0]
        assertThat(migrertArenaAktivitet.id).isEqualTo(tekniskId)
        assertThat(migrertArenaAktivitet.versjon).isEqualTo(versjon)
    }

    @Test
    fun skal_alltid_vere_like_mange_aktiviteter_med_toggle_av_eller_pa() {
        val arenaaktivitetId = ArenaId("ARENATA3123")
        val tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)

        // Default toggle for testene er på
        Mockito.`when`(unleash.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE))
            .thenReturn(false)
        val preMigreringArenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId)
        assertThat(preMigreringArenaAktiviteter).hasSize(0) // ikke 1, fordi alle tiltak er filtrert bort
        val preMigreringVeilarbAktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter
        assertThat(preMigreringVeilarbAktiviteter).isEmpty()

        // Migrer aktivtet
        aktivitetTestService.opprettEksterntArenaKort(
            ArenaKort(
                tiltaksaktivitet,
                arenaMeldingHeaders(mockBruker, arenaaktivitetId)
            )
        )
        // Toggle av, skal ikke vise migrerte aktiviteter
        val toggleAvArenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId)
        assertThat(toggleAvArenaAktiviteter).hasSize(0) // Ikke 1, fordi alle tiltak er filtrert bort
        val toggleAvVeilarbAktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter
        assertThat(toggleAvVeilarbAktiviteter).isEmpty()
        // Toggle på, skal vise migrerte aktiviteter
        Mockito.`when`(unleash.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)).thenReturn(true)
        val togglePaArenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId)
        assertThat(togglePaArenaAktiviteter).isEmpty()
        val togglePaVeilarbAktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter
        assertThat(togglePaVeilarbAktiviteter).hasSize(1)
    }

    @Test
    fun skal_ikke_gi_ut_tiltakaktiviteter_pa_intern_api() {
        val tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)
        aktivitetTestService.opprettEksterntArenaKort(ArenaKort(tiltaksaktivitet, arenaMeldingHeaders(mockBruker)))
        val aktiviteter = aktivitetTestService.hentAktiviteterInternApi(veileder, mockBruker.aktorIdAsAktorId)
        assertThat(aktiviteter).isEmpty()
    }

    @Test
    fun skal_sette_nullable_felt_til_null() {
        val tiltaksaktivitet: Aktivitetskort = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)
            .copy(sluttDato = null, startDato = null, beskrivelse = null)
        aktivitetTestService.opprettEksterntArenaKort(ArenaKort(tiltaksaktivitet, arenaMeldingHeaders(mockBruker)))
        val aktivitet = hentAktivitet(tiltaksaktivitet.id)
        assertThat(aktivitet.fraDato).isNull()
        assertThat(aktivitet.tilDato).isNull()
        assertThat(aktivitet.beskrivelse).isNull()
    }

    @Test
    fun skal_lagre_riktig_identtype_pa_eksterne_aktiviteter() {
        val arbeidsgiverIdent = Ident("123456789", IdentType.ARBEIDSGIVER)
        val arbeidgiverAktivitet: Aktivitetskort = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)
            .copy(endretAv = arbeidsgiverIdent)
        val tiltaksarragoerIdent = Ident("123456780", IdentType.TILTAKSARRANGOER)
        val tiltaksarrangoerAktivitet: Aktivitetskort = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)
            .copy(endretAv = tiltaksarragoerIdent)
        val systemIdent = Ident("123456770", IdentType.SYSTEM)
        val systemAktivitetsKort: Aktivitetskort = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)
            .copy(endretAv = systemIdent)
        aktivitetTestService.opprettEksterntArenaKort(
            listOf(
                ArenaKort(arbeidgiverAktivitet, arenaMeldingHeaders(mockBruker)),
                ArenaKort(tiltaksarrangoerAktivitet, arenaMeldingHeaders(mockBruker)),
                ArenaKort(systemAktivitetsKort, arenaMeldingHeaders(mockBruker))
            )
        )
        val arbeidsAktivitet = hentAktivitet(arbeidgiverAktivitet.id)
        val tilatksarratgoerAktivitet = hentAktivitet(tiltaksarrangoerAktivitet.id)
        val systemAktivitet = hentAktivitet(systemAktivitetsKort.id)
        assertThat(arbeidsAktivitet.endretAv).isEqualTo(arbeidsgiverIdent.ident)
        assertThat(arbeidsAktivitet.endretAvType).isEqualTo(arbeidsgiverIdent.identType.toString())
        assertThat(tilatksarratgoerAktivitet.endretAv).isEqualTo(tiltaksarragoerIdent.ident)
        assertThat(tilatksarratgoerAktivitet.endretAvType)
            .isEqualTo(tiltaksarragoerIdent.identType.toString())
        assertThat(systemAktivitet.endretAv).isEqualTo(systemIdent.ident)
        assertThat(systemAktivitet.endretAvType).isEqualTo(systemIdent.identType.toInnsender().toString())
    }

    @Test
    fun skal_vise_lonnstilskudd_men_ikke_migrerte_arena_aktiviteter_hvis_toggle_er_av() {
        val arenaAktivitet = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)
        val arenata123 = ArenaId("ARENATA123")
        val kontekst = arenaMeldingHeaders(mockBruker, arenata123, "MIDLONNTIL")
        aktivitetTestService.opprettEksterntArenaKort(listOf(ArenaKort(arenaAktivitet, kontekst)))
        val midlertidigLonnstilskudd = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)
        val kafkaAktivitetskortWrapperDTO = KafkaAktivitetskortWrapperDTO(
            midlertidigLonnstilskudd,
            UUID.randomUUID(),
            AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            MessageSource.TEAM_TILTAK
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(kafkaAktivitetskortWrapperDTO))
        Mockito.`when`(unleash.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)).thenReturn(true)
        val alleEksternAktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker)
        assertThat(alleEksternAktiviteter.aktiviteter).hasSize(2)
        Mockito.`when`(unleash.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE))
            .thenReturn(false)
        val eksterneAktiviteterUnntattMigrerteArenaAktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker)
        assertThat(eksterneAktiviteterUnntattMigrerteArenaAktiviteter.aktiviteter).hasSize(1)
        val aktivitet = eksterneAktiviteterUnntattMigrerteArenaAktiviteter.getAktiviteter()[0]
        assertThat(aktivitet.eksternAktivitet.type).isEqualTo(AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD)
    }

    @Test
    fun skal_kunne_kassere_aktiviteter() {
        val tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)
        aktivitetTestService.opprettEksterntArenaKort(ArenaKort(tiltaksaktivitet, arenaMeldingHeaders(mockBruker)))
        val kassering = KasseringsBestilling(
            "team-tiltak",
            UUID.randomUUID(),
//            ActionType.KASSER_AKTIVITET,
            NavIdent.of("z123456"),
            NorskIdent.of("12121212121"),
            tiltaksaktivitet.id,
            "Fordi"
        )
        aktivitetTestService.kasserEskterntAktivitetskort(kassering)
        val aktivitet = aktivitetTestService.hentAktivitetByFunksjonellId(mockBruker, veileder, kassering.aktivitetsId)
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.AVBRUTT)
        assertThat(aktivitet.endretAv).isEqualTo("z123456")
        assertThat(aktivitet.eksternAktivitet.detaljer).isEmpty()
        assertThat(aktivitet.eksternAktivitet.etiketter).isEmpty()
        assertThat(aktivitet.eksternAktivitet.handlinger).isEmpty()
        assertThat(aktivitet.eksternAktivitet.oppgave).isNull()
        val params = MapSqlParameterSource().addValue("aktivitetId", aktivitet.id.toLong())
        val count = namedParameterJdbcTemplate.queryForObject(
            """
                SELECT count(*)
                FROM KASSERT_AKTIVITET 
                WHERE AKTIVITET_ID = :aktivitetId
                
                """.trimIndent(), params, Int::class.javaPrimitiveType!!
        )
        assertThat(count!!).isEqualTo(1)
    }

    @Test
    fun skal_kunne_publisere_feilmelding_nar_man_kasserer_aktivitet_som_ikke_finnes() {
        val funksjonellId = UUID.randomUUID()
        val kassering = KasseringsBestilling(
            MessageSource.TEAM_TILTAK.name,
            UUID.randomUUID(),
            NavIdent.of("z123456"),
            NorskIdent.of("12121212121"),
            funksjonellId,
            "Fordi"
        )
        aktivitetTestService.kasserEskterntAktivitetskort(kassering)
        assertFeilmeldingPublished(
            funksjonellId,
            ErrorType.AKTIVITET_IKKE_FUNNET,
            MessageSource.TEAM_TILTAK
        )
    }

    @Test
    fun `skal lage flere versjoner av acl-aktiviteter som ikke er er tatt over`() {
        // Areneaktivitet med FHO før migrering
        val arenaaktivitetId = ArenaId("TA31212")
        val arenaAktivitetDTO =
            aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, arenaaktivitetId, veileder)
        val funksjonellId = UUID.randomUUID()
        val kontekst = arenaMeldingHeaders(mockBruker, arenaaktivitetId, "ARENA_TILTAK")
        val tiDagerSiden = ZonedDateTime.now().minusDays(10)
        val gammel = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
            .copy(endretTidspunkt = tiDagerSiden)
        val endaEnVersjon = aktivitetskort(funksjonellId, AktivitetskortStatus.GJENNOMFORES)
            .copy(endretTidspunkt = tiDagerSiden.plusDays(5))
        val ny = aktivitetskort(funksjonellId, AktivitetskortStatus.PLANLAGT)
            .copy(endretTidspunkt = ZonedDateTime.now())
        aktivitetTestService.opprettEksterntArenaKort(ArenaKort(gammel, kontekst))
        aktivitetTestService.opprettEksterntArenaKort(ArenaKort(endaEnVersjon, kontekst))
        aktivitetTestService.opprettEksterntArenaKort(ArenaKort(ny, kontekst))

        val initiellAktivitet = hentAktivitet(funksjonellId)
        val versjoner = aktivitetTestService.hentVersjoner(initiellAktivitet.id, mockBruker, mockBruker)
        // Alle versjoner skal finnes
        assertThat(versjoner.map { it.transaksjonsType })
            .containsExactly(
                AktivitetTransaksjonsType.STATUS_ENDRET,
                AktivitetTransaksjonsType.DETALJER_ENDRET,
                AktivitetTransaksjonsType.STATUS_ENDRET,
                AktivitetTransaksjonsType.DETALJER_ENDRET,
                AktivitetTransaksjonsType.OPPRETTET
            )
        assertThat(versjoner.groupBy { it.endretDato }).hasSize(3)
        // Skal bruke første opprettet dato
        val nyeste = versjoner.first()
        assertThat(DateUtils.dateToZonedDateTime(nyeste.opprettetDato)).isCloseTo(
            tiDagerSiden,
            within(1, ChronoUnit.SECONDS)
        )
        assertThat(nyeste.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.STATUS_ENDRET)
        assertThat(nyeste.oppfolgingsperiodeId).isEqualTo(initiellAktivitet.oppfolgingsperiodeId)
        assertThat(nyeste.isHistorisk).isEqualTo(initiellAktivitet.isHistorisk)
        // Behold FHO
        val heleAktiviteten = hentAktivitet(funksjonellId)
        assertThat(arenaAktivitetDTO.forhaandsorientering.id).isEqualTo(heleAktiviteten.forhaandsorientering.id)
    }

    @Test
    fun komet_skal_kunne_ta_over_arenaaktivitet() {
        val tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT)

        aktivitetTestService.opprettEksterntArenaKort(
            listOf(ArenaKort(tiltaksaktivitet, arenaMeldingHeaders(mockBruker)))
        )

        val tiltaksaktivitetEndret =
            tiltaksaktivitet.copy(endretAv = Ident.builder().ident("team_tiltak").identType(IdentType.SYSTEM).build())
        val kometSinAktivitet = KafkaAktivitetskortWrapperDTO(
            tiltaksaktivitetEndret,
            AktivitetskortType.GRUPPEAMO,
            MessageSource.TEAM_KOMET
        )

        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(kometSinAktivitet))

        val aktivitet = hentAktivitet(tiltaksaktivitet.id)
        assertThat(aktivitet.type).isEqualTo(AktivitetTypeDTO.EKSTERNAKTIVITET)
        val oppfolgingsperiode = aktivitet.oppfolgingsperiodeId
        assertThat(oppfolgingsperiode).isNotNull()
        assertThat(aktivitet.eksternAktivitet?.type).isEqualTo(AktivitetskortType.GRUPPEAMO)
        assertThat(aktivitet.endretAv).isEqualTo("team_tiltak")
        assertThat(aktivitet.endretAvType).isEqualTo("SYSTEM")
        assertThat(aktivitet.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.DETALJER_ENDRET) // TODO egen transaksjonstype for denne?

        val aktivitetData = aktivitetskortService.hentAktivitetskortByFunksjonellId(tiltaksaktivitet.id).get()
        assertThat(aktivitetData.oppfolgingsperiodeId).isEqualTo(oppfolgingsperiode)
        val eksternAktivitetData = aktivitetData.eksternAktivitetData!!
        assertThat(eksternAktivitetData.source).isEqualTo(MessageSource.TEAM_KOMET.name)
        assertThat(eksternAktivitetData.tiltaksKode).isNull()
        assertThat(eksternAktivitetData.arenaId).isNull()
    }

    @Test
    fun acl_oppdateringer_skal_ignoreres_hvis_komet_har_tatt_over_aktivitet() {
        val arenaId = ArenaId("ARENATA9988")
        val arenaKortSerie = AktivitetskortSerie(mockBruker, AktivitetskortType.ARENA_TILTAK)
        val arenaAktivitetskortwrapper = arenaKortSerie.ny(AktivitetskortStatus.PLANLAGT)
        aktivitetTestService.opprettEksterntArenaKort(
            listOf(
                ArenaKort(
                    arenaAktivitetskortwrapper,
                    arenaMeldingHeaders(mockBruker, arenaId)
                )
            )
        )

        val kometAktivitetskortWrapperDTO = arenaAktivitetskortwrapper
            .copy(
                messageId = UUID.randomUUID(),
                aktivitetskort = arenaAktivitetskortwrapper.aktivitetskort.copy(
                    endretAv = Ident(
                        "srvamt",
                        IdentType.SYSTEM
                    ), aktivitetStatus = AktivitetskortStatus.GJENNOMFORES
                ),
                aktivitetskortType = AktivitetskortType.GRUPPEAMO,
                source = MessageSource.TEAM_KOMET.name
            )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(kometAktivitetskortWrapperDTO))

        val ignorertArenaAktivitetskort = arenaKortSerie.ny(AktivitetskortStatus.FULLFORT)
        aktivitetTestService.opprettEksterntArenaKort(
            listOf(
                ArenaKort(
                    ignorertArenaAktivitetskort,
                    arenaMeldingHeaders(mockBruker, arenaId)
                )
            )
        )

        val aktivitet = hentAktivitet(arenaKortSerie.funksjonellId)
        assertThat(aktivitet.type).isEqualTo(AktivitetTypeDTO.EKSTERNAKTIVITET)
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.GJENNOMFORES)
        val oppfolgingsperiode = aktivitet.oppfolgingsperiodeId
        assertThat(oppfolgingsperiode).isNotNull()
        assertThat(aktivitet.eksternAktivitet?.type).isEqualTo(AktivitetskortType.GRUPPEAMO)
        assertThat(aktivitet.endretAv).isEqualTo("srvamt")
        assertThat(aktivitet.endretAvType).isEqualTo(IdentType.SYSTEM.name)
        assertThat(aktivitet.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.STATUS_ENDRET) // TODO egen transaksjonstype for denne?

        val aktivitetData = aktivitetskortService.hentAktivitetskortByFunksjonellId(arenaKortSerie.funksjonellId).get()
        assertThat(aktivitetData.oppfolgingsperiodeId).isEqualTo(oppfolgingsperiode)
        val eksternAktivitetData = aktivitetData.eksternAktivitetData!!
        assertThat(eksternAktivitetData.source).isEqualTo(MessageSource.TEAM_KOMET.name)
        assertThat(eksternAktivitetData.tiltaksKode).isNull()
        assertThat(eksternAktivitetData.arenaId).isNull()
    }

    @Test
    fun acl_oppdateringer_skal_migrere_hvis_komet_har_tatt_over_aktivitet_forst() {
        val arenaKortSerie = ArenaAktivitetskortSerie(mockBruker, "GRUPPEAMO")
        val arenaKort = arenaKortSerie.ny(AktivitetskortStatus.PLANLAGT, ZonedDateTime.now())
        val kometAktivitetskortWrapperDTO = arenaKort.melding
            .copy(
                messageId = UUID.randomUUID(),
                aktivitetskort = arenaKort.melding.aktivitetskort
                    .copy(
                        endretAv = Ident("srvamt", IdentType.SYSTEM),
                        aktivitetStatus = AktivitetskortStatus.GJENNOMFORES
                    ),
                aktivitetskortType = AktivitetskortType.GRUPPEAMO,
                source = MessageSource.TEAM_KOMET.name
            )

        // Opprett komet-kort, deretter opprett acl-kort
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(kometAktivitetskortWrapperDTO))
        aktivitetTestService.opprettEksterntArenaKort(listOf(arenaKort))
        val ekstraArenaMelding = arenaKortSerie.ny(AktivitetskortStatus.FULLFORT, ZonedDateTime.now())
        aktivitetTestService.opprettEksterntArenaKort(listOf(ekstraArenaMelding))

        val aktivitet = hentAktivitet(arenaKortSerie.funksjonellId)
        assertThat(aktivitet.type).isEqualTo(AktivitetTypeDTO.EKSTERNAKTIVITET)
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.GJENNOMFORES)
        val oppfolgingsperiode = aktivitet.oppfolgingsperiodeId
        assertThat(oppfolgingsperiode).isNotNull()
        assertThat(aktivitet.eksternAktivitet?.type).isEqualTo(AktivitetskortType.GRUPPEAMO)
        assertThat(aktivitet.endretAv).isEqualTo("srvamt")
        assertThat(aktivitet.endretAvType).isEqualTo(IdentType.SYSTEM.name)
        assertThat(aktivitet.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.OPPRETTET) // TODO egen transaksjonstype for denne?

        val aktivitetData = aktivitetskortService.hentAktivitetskortByFunksjonellId(arenaKortSerie.funksjonellId).get()
        assertThat(aktivitetData.oppfolgingsperiodeId).isEqualTo(oppfolgingsperiode)
        val eksternAktivitetData = aktivitetData.eksternAktivitetData!!
        assertThat(eksternAktivitetData.source).isEqualTo(MessageSource.TEAM_KOMET.name)
        assertThat(eksternAktivitetData.tiltaksKode).isNull()
        assertThat(eksternAktivitetData.arenaId).isNull()
    }
}

fun arenaMeldingHeaders(mockBruker: MockBruker): ArenaMeldingHeaders {
    return ArenaMeldingHeaders(
        ArenaId("ARENATA" + Random.nextInt(10000)),
        "MIDL",
        mockBruker.oppfolgingsperiodeId,
        null
    )
}

fun arenaMeldingHeaders(mockBruker: MockBruker, eksternRefanseId: ArenaId?): ArenaMeldingHeaders {
    return ArenaMeldingHeaders(eksternRefanseId, "MIDL", mockBruker.oppfolgingsperiodeId, null)
}

fun arenaMeldingHeaders(
    mockBruker: MockBruker,
    eksternRefanseId: ArenaId?,
    arenaTiltakskode: String?
): ArenaMeldingHeaders {
    return ArenaMeldingHeaders(eksternRefanseId, arenaTiltakskode, mockBruker.oppfolgingsperiodeId, null)
}