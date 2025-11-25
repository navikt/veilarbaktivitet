package no.nav.veilarbaktivitet.aktivitetskort.service

import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.brukernotifikasjon.MinsideVarselService
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingClient
import no.nav.veilarbaktivitet.oppfolging.periode.*
import no.nav.veilarbaktivitet.oversikten.OversiktenService
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.Person.AktorId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.ZonedDateTime
import java.util.*

class OppfolgingsperiodeServiceTest {
    private lateinit var oppfolgingClient: OppfolgingClient
    private lateinit var aktivitetService: AktivitetService
    private lateinit var minsideVarselService: MinsideVarselService
    private lateinit var sistePeriodeDAO: SistePeriodeDAO
    private lateinit var oppfolgingsperiodeDAO: OppfolgingsperiodeDAO
    private lateinit var oversiktenService: OversiktenService


    private lateinit var oppfolgingsperiodeService: OppfolgingsperiodeService

    companion object {
        private val AKTOR_ID: AktorId = Person.aktorId("123")
        private val DATE_TIME_NOW = ZonedDateTime.now()
    }

    @BeforeEach
    fun setup() {
        oppfolgingClient = Mockito.mock(OppfolgingClient::class.java)
        aktivitetService = Mockito.mock(AktivitetService::class.java)
        minsideVarselService = Mockito.mock(MinsideVarselService::class.java)
        sistePeriodeDAO = Mockito.mock(SistePeriodeDAO::class.java)
        oppfolgingsperiodeDAO = Mockito.mock(OppfolgingsperiodeDAO::class.java)
        oversiktenService = Mockito.mock(OversiktenService::class.java)
        oppfolgingsperiodeService = OppfolgingsperiodeService(aktivitetService, minsideVarselService, sistePeriodeDAO, oppfolgingsperiodeDAO, oppfolgingClient, oversiktenService)
    }

    @Test
    fun `opprettetTidspunkt passer i gammel periode`() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(25)
        val riktigPeriode = oppfperiodeDTO(DATE_TIME_NOW.minusDays(30), DATE_TIME_NOW.minusDays(20))
        val perioder = listOf(
            riktigPeriode,
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(10), null)
        )
        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.oppfolgingsperiodeId).isEqualTo(riktigPeriode.oppfolgingsperiodeId)
    }

    @Test
    fun `opprettetTidspunkt passer i gjeldende periode`() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusHours(10)

        val riktigPeriode = oppfperiodeDTO(DATE_TIME_NOW.minusDays(10), null)
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(30), DATE_TIME_NOW.minusDays(20)),
            riktigPeriode
        )

        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.oppfolgingsperiodeId).isEqualTo(riktigPeriode.oppfolgingsperiodeId)
    }

    @Test
    fun `opprettetTidspunkt passer paa startDato`() { // skal være inklusiv med andre ord
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(10)

        val riktigPeriode = oppfperiodeDTO(DATE_TIME_NOW.minusDays(10), null)
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(30), DATE_TIME_NOW.minusDays(20)),
            riktigPeriode
        )
        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.oppfolgingsperiodeId).isEqualTo(riktigPeriode.oppfolgingsperiodeId)
    }

    @Test
    fun `feil periode er naermere, men foer oppstart`() {
        val opprettetTidspunkt = DATE_TIME_NOW

        val riktigPeriode = oppfperiodeDTO(DATE_TIME_NOW.plusDays(6), null)
        val perioder = listOf(
            riktigPeriode,
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(4), DATE_TIME_NOW.minusDays(2))
        )

        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.oppfolgingsperiodeId).isEqualTo(riktigPeriode.oppfolgingsperiodeId)
    }

    @Test
    fun `opprettetTidspunkti to gamle perioder`() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(15)

        // Er riktig fordi den er "nyere" enn den andre perioden
        val riktigPeriode = oppfperiodeDTO(DATE_TIME_NOW.minusDays(16), DATE_TIME_NOW.minusDays(5))
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(20), DATE_TIME_NOW.minusDays(10)),
            riktigPeriode
        )
        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.oppfolgingsperiodeId).isEqualTo(riktigPeriode.oppfolgingsperiodeId)
    }

    @Test
    fun `opprettetTidspunkt i en gammel og en gjeldende periode`() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(15)
        val riktigPeriode = oppfperiodeDTO(DATE_TIME_NOW.minusDays(16), DATE_TIME_NOW)
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(20), DATE_TIME_NOW.minusDays(10)),
            riktigPeriode
        )
        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.oppfolgingsperiodeId).isEqualTo(riktigPeriode.oppfolgingsperiodeId)
    }

    @Test
    fun `opprettetTidspunkt mot en bruker som ikke har oppfolgingsperioder`() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(15)
        val perioder: List<Oppfolgingsperiode> = listOf()
        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode).isNull()
    }

    @Test
    fun `velg naermeste periode etter opprettetitdspunkt OG som er 10 min innen opprettetTidspunkt`() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(10)

        val riktigPeriode = oppfperiodeDTO(DATE_TIME_NOW.minusDays(10).plusMinutes(5), DATE_TIME_NOW)
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(10).minusMinutes(4), DATE_TIME_NOW.minusDays(10).minusMinutes(2)),
            riktigPeriode
        )

        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.oppfolgingsperiodeId).isEqualTo(riktigPeriode.oppfolgingsperiodeId)
    }

    @Test
    fun `ikke velg periode hvis perioden slutter foer aktivitetens opprettetTidspunkt`() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(10)

        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(10).minusMinutes(5), DATE_TIME_NOW.minusDays(10).minusMinutes(2))
        )

        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode).isNull()
    }

    @Test
    fun `preferer aktiv periode selv om forrige periode sluttet naermere opprettetTidspunkt`() {
        val opprettetTidspunkt = DATE_TIME_NOW

        val riktigPeriode = oppfperiodeDTO(
            opprettetTidspunkt.plusDays(3),
            null
        )
        val perioder = listOf(
            oppfperiodeDTO(
                opprettetTidspunkt.minus(OppfolgingsperiodeService.SLACK_FOER).minusDays(200),
                opprettetTidspunkt.minus(OppfolgingsperiodeService.SLACK_FOER).plusDays(2)
            ),
            riktigPeriode
        )

        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode).isEqualTo(riktigPeriode)
    }

    @Test
    fun `ikke velg periode hvis den slutter mer enn en uke foer opprettetTidspunkt`() {
        val opprettetTidspunkt = DATE_TIME_NOW

        val perioder = listOf(
            oppfperiodeDTO(
                opprettetTidspunkt.minus(OppfolgingsperiodeService.SLACK_FOER).minusDays(10),
                opprettetTidspunkt.minus(OppfolgingsperiodeService.SLACK_FOER).minusDays(1)
            )
        )

        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode).isNull()
    }

    @Test
    fun ti_min_innen_en_gjeldende_periode() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(10)

        val riktigPeriode: Oppfolgingsperiode = oppfperiodeDTO(DATE_TIME_NOW.minusDays(10).plusMinutes(5), null)
        val perioder = listOf(riktigPeriode)
        val oppfolgingsperiode: Oppfolgingsperiode? =
            stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.oppfolgingsperiodeId).isEqualTo(riktigPeriode.oppfolgingsperiodeId)
    }

    @Test
    fun test() {
        val opprettetTidspunkt = ZonedDateTime.parse("2022-03-28T08:20:00.586835+01")
        val riktigPeriode = oppfperiodeDTO(
            ZonedDateTime.parse("2020-12-02T12:44:48.355676+01:00"),
            ZonedDateTime.parse("2022-09-06T00:01:04.457439+02:00")
        )
        val feilPeriode = oppfperiodeDTO(
            ZonedDateTime.parse("2022-09-26T11:10:57.374597+02:00"),
            null
        )
        val perioder = listOf(riktigPeriode, feilPeriode)
        val oppfolgingsperiode =
            stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.oppfolgingsperiodeId).isEqualTo(riktigPeriode.oppfolgingsperiodeId)
    }


    private fun stubOgFinnOppgolgingsperiode(
        perioder: List<Oppfolgingsperiode>,
        opprettetTidspunkt: ZonedDateTime
    ): Oppfolgingsperiode? {
        return perioder.finnOppfolgingsperiodeForTidspunkt(opprettetTidspunkt.toLocalDateTime())
    }

    private fun oppfperiodeDTO(startDato: ZonedDateTime, sluttDato: ZonedDateTime?): Oppfolgingsperiode {
        return Oppfolgingsperiode(AKTOR_ID.get(), UUID.randomUUID(), startDato, sluttDato)
    }
}

