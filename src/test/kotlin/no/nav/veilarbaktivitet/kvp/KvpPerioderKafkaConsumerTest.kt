package no.nav.veilarbaktivitet.kvp

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class KvpPerioderKafkaConsumerTest : SpringBootTestBase() {
    private val kvpEnhetId = "1337"
    private lateinit var mockBrukerMedKvp: MockBruker
    private lateinit var mockVeileder: MockVeileder

    @Autowired
    private lateinit var producer: KafkaTemplate<String, String>

    @Value("\${topic.inn.kvpPerioder}")
    private lateinit var kvpPerioderTopic: String

    @Value("\${topic.inn.kvpAvsluttet}")
    private lateinit var kvpAvsluttetTopic: String

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
            kvpAvsluttetJson("c5b85f02-0d8e-42d7-af4f-d8f10af6f71e", ZonedDateTime.now().plusMinutes(1))
        )

        val oppdatertAktivitet = aktivitetTestService.hentAktivitet(mockBrukerMedKvp, aktivitet.id)
        assertThat(oppdatertAktivitet.status).isEqualTo(AktivitetStatus.AVBRUTT)
    }

    @Test
    fun `skal avbryte aktiviteter ved avsluttet hendelse fra gammel topic`() {
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBrukerMedKvp, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
        assertAktivitetHarForventetKontorsperreEnhet(aktivitet.id.toLong(), kvpEnhetId)

        publiserKvpAvsluttetHendelse(kvpAvsluttetGammelJson(ZonedDateTime.now().plusMinutes(1)))

        val oppdatertAktivitet = aktivitetTestService.hentAktivitet(mockBrukerMedKvp, aktivitet.id)
        assertThat(oppdatertAktivitet.status).isEqualTo(AktivitetStatus.AVBRUTT)
    }

    @Test
    fun `skal ikke endre aktiviteter ved startet hendelse`() {
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBrukerMedKvp, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
        assertAktivitetHarForventetKontorsperreEnhet(aktivitet.id.toLong(), kvpEnhetId)

        publiserKvpPerioderHendelse(
            kvpStartetJson("b53f68f7-3aeb-457e-a66f-6dd6c3bc8e31", ZonedDateTime.now())
        )

        val oppdatertAktivitet = aktivitetTestService.hentAktivitet(mockBrukerMedKvp, aktivitet.id)
        assertThat(oppdatertAktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
    }

    @Test
    fun `skal handtere avsluttet mellom gammel og ny topic uten sideeffekt`() {
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBrukerMedKvp, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        val avsluttetDato = ZonedDateTime.now().plusMinutes(1)
        val kontorsperreEnhetId = hentKontorsperreEnhetId(aktivitet.id.toLong())
        assertThat(kontorsperreEnhetId).isEqualTo(kvpEnhetId)

        publiserKvpAvsluttetHendelse(kvpAvsluttetGammelJson(avsluttetDato))
        publiserKvpPerioderHendelse(
            kvpAvsluttetJson("f3efe6c2-04f5-4693-aa4d-87345ec5ac71", avsluttetDato, kontorsperreEnhetId)
        )

        assertThat(aktivitetTestService.hentAktivitet(mockBrukerMedKvp, aktivitet.id).status).isEqualTo(AktivitetStatus.AVBRUTT)
    }

    @Test
    fun `skal ikke avslutte aktivitet nar enhet ikke matcher kontorsperre`() {
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBrukerMedKvp, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        val avsluttetDato = ZonedDateTime.now().plusMinutes(1)
        assertAktivitetHarForventetKontorsperreEnhet(aktivitet.id.toLong(), kvpEnhetId)

        publiserKvpPerioderHendelse(
            kvpAvsluttetJson("db0bbdcf-1f70-46b0-bbff-7b5f2aaae117", avsluttetDato, "9999")
        )
        assertThat(aktivitetTestService.hentAktivitet(mockBrukerMedKvp, aktivitet.id).status).isEqualTo(AktivitetStatus.PLANLAGT)
    }

    private fun kvpStartetJson(uuid: String, startetDato: ZonedDateTime): String {
        val enhetId = kvpEnhetId
        val sluttDato = startetDato.plusDays(14)
        return """
            {
              "uuid": "$uuid",
              "aktorId": "${mockBrukerMedKvp.aktorId.get()}",
              "event": "STARTET",
              "enhetId": "$enhetId",
              "startet": "${startetDato.toOffsetDateTime()}",
              "avsluttet": "${sluttDato.toOffsetDateTime()}",
              "startDato": "${startetDato.toOffsetDateTime()}",
              "sluttDato": "${sluttDato.toOffsetDateTime()}"
            }
        """.trimIndent()
    }

    private fun kvpAvsluttetJson(uuid: String, avsluttetDato: ZonedDateTime, enhetId: String? = null): String {
        val startDato = avsluttetDato.minusDays(14)
        val enhet = enhetId ?: kvpEnhetId
        return """
            {
              "uuid": "$uuid",
              "aktorId": "${mockBrukerMedKvp.aktorId.get()}",
              "event": "AVSLUTTET",
              "enhetId": "$enhet",
              "startet": "${startDato.toOffsetDateTime()}",
              "avsluttet": "${avsluttetDato.toOffsetDateTime()}",
              "startDato": "${startDato.toOffsetDateTime()}",
              "sluttDato": "${avsluttetDato.toOffsetDateTime()}"
            }
        """.trimIndent()
    }

    private fun kvpAvsluttetGammelJson(avsluttetDato: ZonedDateTime): String {
        return """
            {
              "aktorId": "${mockBrukerMedKvp.aktorId.get()}",
              "avsluttetAv": "${mockVeileder.navIdent}",
              "avsluttetDato": "${avsluttetDato.toOffsetDateTime()}",
              "avsluttetBegrunnelse": "test"
            }
        """.trimIndent()
    }

    private fun publiserKvpPerioderHendelse(rawJson: String) {
        val offset = producer.send(kvpPerioderTopic, rawJson).get(2, TimeUnit.SECONDS).recordMetadata.offset()
        kafkaTestService.assertErKonsumert(kvpPerioderTopic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, offset)
    }

    private fun publiserKvpAvsluttetHendelse(rawJson: String) {
        val offset = producer.send(kvpAvsluttetTopic, rawJson).get(2, TimeUnit.SECONDS).recordMetadata.offset()
        kafkaTestService.assertErKonsumert(kvpAvsluttetTopic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, offset)
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
