package no.nav.veilarbaktivitet.admin

import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType.KASSERT
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortUtil
import no.nav.veilarbaktivitet.aktivitetskort.ArenaKort
import no.nav.veilarbaktivitet.aktivitetskort.ArenaMeldingHeaders
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortStatus
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import no.nav.veilarbaktivitet.util.KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION
import no.nav.veilarbaktivitet.veilarbportefolje.dto.KafkaAktivitetMeldingV4
import org.apache.kafka.clients.consumer.Consumer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord
import java.time.ZonedDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KasserControllerTest : SpringBootTestBase() {

    private lateinit var mockBruker: MockBruker
    private lateinit var veilederSomKanKassere: MockVeileder
    private lateinit var portefoljeConsumer: Consumer<String, String>

    @Value("\${topic.ut.portefolje}")
    private lateinit var portefoljeTopic: String

    @BeforeAll
    fun beforeAll() {
        mockBruker = navMockService.createBruker(BrukerOptions.happyBruker())
        veilederSomKanKassere = navMockService.createVeileder(ident = "Z999999", mockBruker = mockBruker)
    }

    @BeforeEach
    fun beforeEach() {
        portefoljeConsumer = kafkaTestService.createStringStringConsumer(portefoljeTopic)
    }

    @AfterEach
    fun afterEach() {
        portefoljeConsumer.unsubscribe()
        portefoljeConsumer.close()
    }

    @Test
    fun `skal kke kunne kassere aktivitet uten tilgang`() {
        val veilederUtenKasseringstilgang = navMockService.createVeileder(mockBruker = mockBruker)
        val aktivitet = aktivitetTestService.opprettAktivitetViaHttp(
            mockBruker, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.STILLING
            )
        )

        veilederUtenKasseringstilgang
            .createRequest()
            .put("http://localhost:" + port + "/veilarbaktivitet/api/kassering/" + aktivitet.id)
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun `skal lage ny versjon av aktivitet ved kassering`() {
        val opprettetAktivitet = aktivitetTestService.opprettAktivitetViaHttp(
            mockBruker, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.STILLING
            )
        )
        val aktivitetId = opprettetAktivitet.id.toLong()
        val versjonerFørKassering = aktivitetTestService.hentVersjoner(aktivitetId.toString(), mockBruker, veilederSomKanKassere)
        assertThat(versjonerFørKassering.size).isEqualTo(1)

        aktivitetTestService.kasserAktivitetskort(veilederSomKanKassere, opprettetAktivitet.id)

        val versjonerEtterKassering = aktivitetTestService.hentVersjoner(aktivitetId.toString(), mockBruker, veilederSomKanKassere)
        assertThat(versjonerEtterKassering.size).isEqualTo(2)
        val kassertVersjon = versjonerEtterKassering.find { it.transaksjonsType == KASSERT }!!
        assertThat(kassertVersjon.endretAv).isEqualTo(veilederSomKanKassere.navIdent)
        assertThat(kassertVersjon.endretAvType).isEqualTo(Innsender.NAV.toString())
        assertThat(kassertVersjon.endretDato).isCloseTo(Date(), 1000)
    }

    @Test
    fun `skal overskrive bestemte felter på alle versjoner av aktiviteten når man kasserer`() {
        val opprettetAktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.STILLING))
        val aktivitetId = opprettetAktivitet.id

        aktivitetTestService.kasserAktivitetskort(veilederSomKanKassere, opprettetAktivitet.id)

        val versjoner = aktivitetTestService.hentVersjoner(aktivitetId.toString(), mockBruker, veilederSomKanKassere)
        versjoner.forEach { aktivitetVersjon ->
            assertThat(aktivitetVersjon.tittel).isEqualTo("Det var skrevet noe feil, og det er nå slettet")
            assertThat(aktivitetVersjon.avsluttetKommentar).isEqualTo("Kassert av NAV")
            assertThat(aktivitetVersjon.lenke).isEqualTo("Kassert av NAV")
            assertThat(aktivitetVersjon.beskrivelse).isEqualTo("Kassert av NAV")
        }
    }

    @Test
    fun `skal ikke overskrive endret-felter og statusfelt på tidligere versjoner av aktiviteten når man kasserer`() {
        val annenVeileder = navMockService.createVeileder(mockBruker = mockBruker)
        val opprettetAktivitet = aktivitetTestService.opprettAktivitetViaHttp(mockBruker, annenVeileder, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.STILLING))
        val aktivitetId = opprettetAktivitet.id

        aktivitetTestService.kasserAktivitetskort(veilederSomKanKassere, opprettetAktivitet.id)

        val versjoner = aktivitetTestService.hentVersjoner(aktivitetId.toString(), mockBruker, veilederSomKanKassere)
        val sorterteVersjoner = versjoner.sortedBy { it.endretDato }
        val førsteVersjon = sorterteVersjoner.first()
        assertThat(førsteVersjon.status).isEqualTo(AktivitetStatus.PLANLAGT)
        assertThat(førsteVersjon.endretAv).isEqualTo(annenVeileder.navIdent)
        val andreVersjon = sorterteVersjoner[1]
        assertThat(andreVersjon.status).isEqualTo(AktivitetStatus.AVBRUTT)
        assertThat(andreVersjon.endretAv).isEqualTo(veilederSomKanKassere.navIdent)
    }

    @Test
    fun `skal sende avbrutt melding til portefolje ved sletting av eksternaktivitet`() {
        val aktivitetskort = AktivitetskortUtil.ny(
            UUID.randomUUID(),
            AktivitetskortStatus.PLANLAGT,
            ZonedDateTime.now(),
            mockBruker
        )
        val wrapper = KafkaAktivitetskortWrapperDTO(
            payload = aktivitetskort,
            aktivitetskortType = AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            source = MessageSource.TEAM_TILTAK
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(wrapper))
        val opprettetAktivitet = aktivitetTestService.hentAktivitetByFunksjonellId(
            mockBruker,
            veilederSomKanKassere,
            aktivitetskort.id
        )

        aktivitetTestService.slettAktivitet(veilederSomKanKassere, opprettetAktivitet.id)

        val kafkaRecord = getSingleRecord(portefoljeConsumer, portefoljeTopic, DEFAULT_WAIT_TIMEOUT_DURATION)
        val kafkaMelding = JsonUtils.fromJson(kafkaRecord.value(), KafkaAktivitetMeldingV4::class.java)

        assertThat(kafkaMelding.aktivitetId).isEqualTo(opprettetAktivitet.id)
        assertThat(kafkaMelding.aktivitetStatus).isEqualTo(AktivitetStatus.AVBRUTT)
        assertThat(kafkaMelding.oppfolgingsperiodeId).isEqualTo(mockBruker.oppfolgingsperiodeId.toString())

        val antallAktivitetRader = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM AKTIVITET WHERE AKTIVITET_ID = ?",
            Long::class.java,
            opprettetAktivitet.id.toLong()
        )
        assertThat(antallAktivitetRader).isZero()
    }

    @Test
    fun `skal ikke kunne slette aktivitet som ikke er eksternaktivitet`() {
        val opprettetAktivitet = aktivitetTestService.opprettAktivitetViaHttp(
            mockBruker,
            AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.STILLING)
        )

        veilederSomKanKassere
            .createRequest()
            .delete("http://localhost:$port/veilarbaktivitet/api/admin/aktiviteter/${opprettetAktivitet.id}")
            .then()
            .assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }

    @Test
    fun `skal ikke kunne slette eksternaktivitet med forhandsorientering`() {
        val arenaId = ArenaId("TA999999")
        val aktivitetskort = AktivitetskortUtil.ny(
            UUID.randomUUID(),
            AktivitetskortStatus.PLANLAGT,
            ZonedDateTime.now(),
            mockBruker
        )
        val wrapper = KafkaAktivitetskortWrapperDTO(
            payload = aktivitetskort,
            aktivitetskortType = AktivitetskortType.ARENA_TILTAK,
            source = MessageSource.ARENA_TILTAK_AKTIVITET_ACL
        )
        aktivitetTestService.opprettEksterntArenaKort(
            ArenaKort(wrapper, ArenaMeldingHeaders(arenaId, "MIDLONTIL", mockBruker.oppfolgingsperiodeId, null))
        )
        aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, arenaId, veilederSomKanKassere)
        val opprettetAktivitet = aktivitetTestService.hentAktivitetByFunksjonellId(
            mockBruker,
            veilederSomKanKassere,
            aktivitetskort.id
        )

        veilederSomKanKassere
            .createRequest()
            .delete("http://localhost:$port/veilarbaktivitet/api/admin/aktiviteter/${opprettetAktivitet.id}")
            .then()
            .assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value())
    }
}
