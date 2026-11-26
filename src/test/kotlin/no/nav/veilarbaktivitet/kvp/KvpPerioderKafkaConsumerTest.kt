package no.nav.veilarbaktivitet.kvp

import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortUtil
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortStatus
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import no.nav.veilarbaktivitet.util.KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

class KvpPerioderKafkaConsumerTest : SpringBootTestBase() {
    private val kvpEnhetId = "1337"
    private lateinit var mockBrukerMedKvp: MockBruker
    private lateinit var mockVeileder: MockVeileder

    @Autowired
    private lateinit var producer: KafkaTemplate<String, String>

    @Value("\${topic.inn.kvpPerioder}")
    private lateinit var kvpPerioderTopic: String

    @Value("\${topic.ut.siste-aktivitet-hendelse}")
    private lateinit var sisteAktivitetHendelseTopic: String

    @BeforeEach
    fun before() {
        val medKvp = BrukerOptions.happyBruker().toBuilder()
            .erUnderKvp(true)
            .oppfolgingsenhet(kvpEnhetId)
            .build()
        mockBrukerMedKvp = navMockService.createBruker(medKvp)
        mockVeileder = navMockService.createVeileder(mockBrukerMedKvp)
    }

    @Test
    fun `skal avbryte aktiviteter ved avsluttet hendelse fra ny topic`() {
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBrukerMedKvp, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
        assertAktivitetHarForventetKontorsperreEnhet(aktivitet.id.toLong(), kvpEnhetId)

        publiserKvpPerioderHendelse(
            kvpAvsluttetJson(ZonedDateTime.now().plusMinutes(1))
        )

        val oppdatertAktivitet = aktivitetTestService.hentAktivitet(mockBrukerMedKvp, aktivitet.id)
        assertThat(oppdatertAktivitet.status).isEqualTo(AktivitetStatus.AVBRUTT)
    }

    @Test
    fun `skal ikke endre aktiviteter ved startet hendelse`() {
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBrukerMedKvp, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
        assertAktivitetHarForventetKontorsperreEnhet(aktivitet.id.toLong(), kvpEnhetId)

        publiserKvpPerioderHendelse(
            kvpStartetJson(ZonedDateTime.now())
        )

        val oppdatertAktivitet = aktivitetTestService.hentAktivitet(mockBrukerMedKvp, aktivitet.id)
        assertThat(oppdatertAktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
    }

    @Test
    fun `skal ikke avbryte aktiviteter ved gammel avsluttet hendelse på ny topic`() {
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBrukerMedKvp, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
        assertAktivitetHarForventetKontorsperreEnhet(aktivitet.id.toLong(), kvpEnhetId)

        publiserKvpPerioderHendelse(
            kvpAvsluttetJson(ZonedDateTime.of(2026, 10, 12, 23, 59, 59, 0, ZoneId.of("Europe/Oslo")))
        )

        val oppdatertAktivitet = aktivitetTestService.hentAktivitet(mockBrukerMedKvp, aktivitet.id)
        assertThat(oppdatertAktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
    }

    @Test
    fun `skal ikke avbryte eksterne aktiviteter ved avsluttet hendelse`() {
        val aktivitetskort = AktivitetskortUtil.ny(UUID.randomUUID(), AktivitetskortStatus.PLANLAGT, ZonedDateTime.now(), mockBrukerMedKvp)
        val kafkaAktivitetskortWrapperDTO = KafkaAktivitetskortWrapperDTO(
            aktivitetskort, UUID.randomUUID(), AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD, MessageSource.TEAM_TILTAK
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(kafkaAktivitetskortWrapperDTO))

        val opprettetAktivitet = aktivitetTestService.hentAktivitetByFunksjonellId(
            mockBrukerMedKvp,
            mockVeileder,
            aktivitetskort.id
        )
        assertThat(opprettetAktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)

        publiserKvpPerioderHendelse(kvpAvsluttetJson(ZonedDateTime.now().plusMinutes(1)))

        val oppdatertAktivitet = aktivitetTestService.hentAktivitet(mockBrukerMedKvp, opprettetAktivitet.id)
        assertThat(oppdatertAktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
    }

    @Test
    fun `avsluttet KVP skal fjerne alle kontorsperrede møter fra siste-aktivitet topic med en melding`() {
        val sisteAktivitetConsumer = kafkaTestService.createStringStringConsumer(sisteAktivitetHendelseTopic)
        sisteAktivitetConsumer.use { consumer ->
            val møtetidspunkt = ZonedDateTime.now().plusDays(3).truncatedTo(ChronoUnit.SECONDS)
            val møterMedMeldinger = listOf(møtetidspunkt, møtetidspunkt.minusDays(1)).map { tidspunkt ->
                val møte = aktivitetTestService.opprettAktivitetViaHttp(
                    mockBrukerMedKvp,
                    mockVeileder,
                    AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.MOTE)
                        .setFraDato(Date.from(tidspunkt.toInstant()))
                        .setTilDato(Date.from(tidspunkt.plusHours(1).toInstant()))
                )
                val meldingEtterOpprettelse = KafkaTestUtils.getSingleRecord(consumer, sisteAktivitetHendelseTopic, DEFAULT_WAIT_TIMEOUT_DURATION)
                møte to meldingEtterOpprettelse
            }

            publiserKvpPerioderHendelse(kvpAvsluttetJson(ZonedDateTime.now().plusMinutes(1)))

            møterMedMeldinger.forEach { (møte, meldingEtterOpprettelse) ->
                assertAktivitetHarForventetKontorsperreEnhet(møte.id.toLong(), kvpEnhetId)
                assertThat(meldingEtterOpprettelse.key()).isEqualTo(mockBrukerMedKvp.aktorId.get())
                assertNestePlanlagteMøte(meldingEtterOpprettelse.value(), møte.id.toLong(), møte.fraDato.toInstant())
                assertThat(aktivitetTestService.hentAktivitet(mockBrukerMedKvp, mockVeileder, møte.id).status)
                    .isEqualTo(AktivitetStatus.AVBRUTT)
            }
            val meldingEtterKvpAvsluttet = KafkaTestUtils.getSingleRecord(
                consumer, sisteAktivitetHendelseTopic, DEFAULT_WAIT_TIMEOUT_DURATION
            )
            assertThat(meldingEtterKvpAvsluttet.key()).isEqualTo(mockBrukerMedKvp.aktorId.get())
            assertThat(meldingEtterKvpAvsluttet.offset()).isGreaterThan(møterMedMeldinger.last().second.offset())
            assertThat(meldingEtterKvpAvsluttet.value()).isNull()

            val ekstraMeldinger = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1))
                .records(sisteAktivitetHendelseTopic)
                .filter { it.key() == mockBrukerMedKvp.aktorId.get() }
            assertThat(ekstraMeldinger).isEmpty()
        }
    }

    private fun assertNestePlanlagteMøte(json: String, forventetId: Long, forventetTidspunkt: Instant) {
        val nestePlanlagteMote = JsonUtils.getMapper().readTree(json).get("nestePlanlagteMote")
        assertThat(nestePlanlagteMote.get("id").asLong()).isEqualTo(forventetId)
        assertThat(Instant.ofEpochSecond(nestePlanlagteMote.get("tidspunkt").asLong())).isEqualTo(forventetTidspunkt)
    }

    private fun kvpStartetJson(startetDato: ZonedDateTime): String {
        val enhetId = kvpEnhetId
        return """
            {
              "aktorId": "${mockBrukerMedKvp.aktorId.get()}",
              "event": "STARTET",
              "enhetId": "$enhetId",
              "startet": {
                "opprettetAv": "${mockVeileder.navIdent}",
                "opprettetDato": "${startetDato.toOffsetDateTime()}",
                "opprettetBegrunnelse": "startet i test"
              },
              "avsluttet": null
            }
        """.trimIndent()
    }

    private fun kvpAvsluttetJson(avsluttetDato: ZonedDateTime, enhetId: String? = null): String {
        val startDato = avsluttetDato.minusDays(14)
        val enhet = enhetId ?: kvpEnhetId
        return """
            {
              "aktorId": "${mockBrukerMedKvp.aktorId.get()}",
              "event": "AVSLUTTET",
              "enhetId": "$enhet",
              "startet": {
                "opprettetAv": "${mockVeileder.navIdent}",
                "opprettetDato": "${startDato.toOffsetDateTime()}",
                "opprettetBegrunnelse": "startet i test"
              },
              "avsluttet": {
                "avsluttetAv": "${mockVeileder.navIdent}",
                "avsluttetDato": "${avsluttetDato.toOffsetDateTime()}",
                "avsluttetBegrunnelse": "test"
              }
            }
        """.trimIndent()
    }

    private fun publiserKvpPerioderHendelse(rawJson: String) {
        kafkaTestService.assertConsumerGroupLytterPaTopic(kvpPerioderTopic, NavCommonKafkaConfig.CONSUMER_GROUP_ID)
        val offset = producer.send(kvpPerioderTopic, rawJson).get(2, TimeUnit.SECONDS).recordMetadata.offset()
        kafkaTestService.assertErKonsumert(kvpPerioderTopic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, offset)
    }

    private fun hentKontorsperreEnhetId(aktivitetId: Long): String {
        return jdbcTemplate.queryForObject(
            "SELECT kontorsperre_enhet_id FROM AKTIVITET WHERE aktivitet_id = ? AND gjeldende = 1",
            String::class.java,
            aktivitetId
        )!!
    }

    private fun assertAktivitetHarForventetKontorsperreEnhet(aktivitetId: Long, forventetEnhet: String) {
        assertThat(hentKontorsperreEnhetId(aktivitetId)).isEqualTo(forventetEnhet)
    }
}
