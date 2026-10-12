package no.nav.veilarbaktivitet.kvp

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class KvpPerioderKafkaConsumerTest : SpringBootTestBase() {
    private lateinit var mockBruker: MockBruker
    private lateinit var mockVeileder: MockVeileder

    @Autowired
    @Qualifier("navCommonKafkaJsonTemplate")
    private lateinit var navCommonKafkaJsonTemplate: KafkaJsonTemplate<KvpPerioderKafkaDTO>

    @Autowired
    @Qualifier("navCommonKafkaJsonTemplate")
    private lateinit var kvpAvsluttetKafkaJsonTemplate: KafkaJsonTemplate<KvpAvsluttetKafkaDTO>

    @Value("\${topic.inn.kvpPerioder}")
    private lateinit var kvpPerioderTopic: String

    @Value("\${topic.inn.kvpAvsluttet}")
    private lateinit var kvpAvsluttetTopic: String

    @BeforeEach
    fun before() {
        val medKvp = BrukerOptions.happyBruker().toBuilder().erUnderKvp(true).build()
        mockBruker = navMockService.createBruker(medKvp)
        mockVeileder = navMockService.createVeileder(mockBruker)
    }

    @Test
    fun `skal avbryte aktiviteter ved avsluttet hendelse`() {
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)

        publiserKvpPerioderHendelse(kvpAvsluttet("c5b85f02-0d8e-42d7-af4f-d8f10af6f71e", ZonedDateTime.now().plusMinutes(1)))

        val oppdatertAktivitet = aktivitetTestService.hentAktivitet(mockBruker, aktivitet.id)
        assertThat(oppdatertAktivitet.status).isEqualTo(AktivitetStatus.AVBRUTT)
    }

    @Test
    fun `skal ikke endre aktiviteter ved startet hendelse`() {
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)

        publiserKvpPerioderHendelse(kvpStartet("b53f68f7-3aeb-457e-a66f-6dd6c3bc8e31", ZonedDateTime.now()))

        val oppdatertAktivitet = aktivitetTestService.hentAktivitet(mockBruker, aktivitet.id)
        assertThat(oppdatertAktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
    }

    @Test
    fun `skal deduplisere avsluttet mellom gammel og ny topic`() {
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        val avsluttetDato = ZonedDateTime.now().plusMinutes(1)

        publiserKvpAvsluttetHendelse(lagGammelAvsluttetHendelse(avsluttetDato))
        publiserKvpPerioderHendelse(kvpAvsluttet("f3efe6c2-04f5-4693-aa4d-87345ec5ac71", avsluttetDato))

        val dedupKey = "AVSLUTTET:${mockBruker.aktorId.get()}:${avsluttetDato.toInstant().toEpochMilli()}"
        assertThat(hentAntallProsesserteHendelser(dedupKey)).isEqualTo(1)
        assertThat(aktivitetTestService.hentAktivitet(mockBruker, aktivitet.id).status).isEqualTo(AktivitetStatus.AVBRUTT)
    }

    @Test
    fun `skal deduplisere startet på ny topic`() {
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.EGEN))
        val startetDato = ZonedDateTime.now()

        publiserKvpPerioderHendelse(kvpStartet("f0e4d76a-46e0-4de9-ab13-12d9d924dc9c", startetDato))
        publiserKvpPerioderHendelse(kvpStartet("3d11f675-44a8-48d5-80da-df264f2d498a", startetDato))

        val dedupKey = "STARTET:${mockBruker.aktorId.get()}:${startetDato.toInstant().toEpochMilli()}"
        assertThat(hentAntallProsesserteHendelser(dedupKey)).isEqualTo(1)
        assertThat(aktivitetTestService.hentAktivitet(mockBruker, aktivitet.id).status).isEqualTo(AktivitetStatus.PLANLAGT)
    }

    private fun kvpStartet(uuid: String, startetDato: ZonedDateTime): KvpPerioderKafkaDTO {
        return KvpPerioderKafkaDTO().apply {
            this.uuid = uuid
            aktorId = mockBruker.aktorId.get()
            event = KvpPeriodeEvent.STARTET
            startet = startetDato
        }
    }

    private fun kvpAvsluttet(uuid: String, avsluttetDato: ZonedDateTime): KvpPerioderKafkaDTO {
        return KvpPerioderKafkaDTO().apply {
            this.uuid = uuid
            aktorId = mockBruker.aktorId.get()
            event = KvpPeriodeEvent.AVSLUTTET
            avsluttet = avsluttetDato
        }
    }

    private fun lagGammelAvsluttetHendelse(avsluttetDato: ZonedDateTime): KvpAvsluttetKafkaDTO {
        return KvpAvsluttetKafkaDTO()
            .setAktorId(mockBruker.aktorId.get())
            .setAvsluttetBegrunnelse("test")
            .setAvsluttetDato(avsluttetDato)
    }

    private fun publiserKvpPerioderHendelse(hendelse: KvpPerioderKafkaDTO) {
        val offset = navCommonKafkaJsonTemplate.send(kvpPerioderTopic, hendelse).get(2, TimeUnit.SECONDS).recordMetadata.offset()
        kafkaTestService.assertErKonsumert(kvpPerioderTopic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, offset)
    }

    private fun publiserKvpAvsluttetHendelse(hendelse: KvpAvsluttetKafkaDTO) {
        val offset = kvpAvsluttetKafkaJsonTemplate.send(kvpAvsluttetTopic, hendelse).get(2, TimeUnit.SECONDS).recordMetadata.offset()
        kafkaTestService.assertErKonsumert(kvpAvsluttetTopic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, offset)
    }

    private fun hentAntallProsesserteHendelser(dedupKey: String): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM KVP_HENDELSE_PROSESSERT WHERE DEDUP_KEY = ?",
            Int::class.java,
            dedupKey
        ) ?: 0
    }
}
