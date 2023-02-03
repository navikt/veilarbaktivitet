package no.nav.veilarbaktivitet.aktivitetskort.service

import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.Person.AktorId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import java.time.ZonedDateTime
import java.util.*

class OppfolgingsperiodeServiceTest {
    private lateinit var oppfolgingClient: OppfolgingV2Client
    private lateinit var oppfolgingsperiodeService: OppfolgingsperiodeService

    companion object {
        private val AKTOR_ID: AktorId = Person.aktorId("123")
        private val DATE_TIME_NOW = ZonedDateTime.now()
    }

    @BeforeEach
    fun setup() {
        oppfolgingClient = Mockito.mock(OppfolgingV2Client::class.java)
        oppfolgingsperiodeService = OppfolgingsperiodeService(oppfolgingClient)
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
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
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
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    @Test
    fun `opprettetTidspunkt passer paa startDato`() { // skal være inklusiv med andre ord
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(10)

        val riktigPeriode: OppfolgingPeriodeMinimalDTO = oppfperiodeDTO(DATE_TIME_NOW.minusDays(10), null)
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(30), DATE_TIME_NOW.minusDays(20)),
            riktigPeriode
        )
        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    @Test
    @Disabled("Disable til etter inntagelse av LONNSTILSKUDD")
    fun `feil periode er naermere, men foer oppstart`() {
        val opprettetTidspunkt = DATE_TIME_NOW

        val riktigPeriode: OppfolgingPeriodeMinimalDTO = oppfperiodeDTO(DATE_TIME_NOW.plusDays(6), null)
        val perioder = listOf(
            riktigPeriode,
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(4), DATE_TIME_NOW.minusDays(2))
        )

        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    @Test
    fun `opprettetTidspunkti to gamle perioder`() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(15)

        // Er riktig fordi den er "nyere" enn den andre perioden
        val riktigPeriode: OppfolgingPeriodeMinimalDTO = oppfperiodeDTO(DATE_TIME_NOW.minusDays(16), DATE_TIME_NOW.minusDays(5))
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(20), DATE_TIME_NOW.minusDays(10)),
            riktigPeriode
        )
        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    @Test
    fun `opprettetTidspunkt i en gammel og en gjeldende periode`() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(15)
        val riktigPeriode: OppfolgingPeriodeMinimalDTO = oppfperiodeDTO(DATE_TIME_NOW.minusDays(16), DATE_TIME_NOW)
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(20), DATE_TIME_NOW.minusDays(10)),
            riktigPeriode
        )
        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    @Test
    fun `opprettetTidspunkt mot en bruker som ikke har oppfolgingsperioder`() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(15)
        val perioder: List<OppfolgingPeriodeMinimalDTO> = listOf()
        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode).isNull()
    }

    @Test
    @Disabled("Disable til etter inntagelse av LONNSTILSKUDD")
    fun `velg naermeste periode etter opprettetitdspunkt OG som er 10 min innen opprettetTidspunkt`() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(10)

        val riktigPeriode = oppfperiodeDTO(DATE_TIME_NOW.minusDays(10).plusMinutes(5), DATE_TIME_NOW)
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(10).minusMinutes(4), DATE_TIME_NOW.minusDays(10).minusMinutes(2)),
            riktigPeriode
        )

        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    @Test
    @Disabled("Disable til etter inntagelse av LONNSTILSKUDD")
    fun `ikke velg periode hvis perioden slutter foer aktivitetens opprettetTidspunkt`() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(10)

        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME_NOW.minusDays(10).minusMinutes(5), DATE_TIME_NOW.minusDays(10).minusMinutes(2))
        )

        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode).isNull()
    }

    @Test
    fun `velg periode hvis den slutter mindre enn en uke foer opprettetTidspunkt`() {
        val opprettetTidspunkt = DATE_TIME_NOW

        val riktigPeriode = oppfperiodeDTO(
            opprettetTidspunkt.minus(OppfolgingsperiodeService.SLACK_FOER).minusDays(2),
            opprettetTidspunkt.minus(OppfolgingsperiodeService.SLACK_FOER).plusDays(1)
        )
        val perioder = listOf(
            riktigPeriode,
            oppfperiodeDTO(
                opprettetTidspunkt.plusDays(10),
                opprettetTidspunkt.plusDays(15)
            )
        )

        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode).isEqualTo(riktigPeriode)
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
                opprettetTidspunkt.minus(OppfolgingsperiodeService.SLACK_FOER)
            )
        )

        val oppfolgingsperiode = stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode).isNull()
    }

    @Test
    fun ti_min_innen_en_gjeldende_periode() {
        val opprettetTidspunkt = DATE_TIME_NOW.minusDays(10)

        val riktigPeriode: OppfolgingPeriodeMinimalDTO = oppfperiodeDTO(DATE_TIME_NOW.minusDays(10).plusMinutes(5), null)
        val perioder = listOf(riktigPeriode)
        val oppfolgingsperiode: OppfolgingPeriodeMinimalDTO? =
            stubOgFinnOppgolgingsperiode(perioder, opprettetTidspunkt)
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }


    private fun stubOgFinnOppgolgingsperiode(
        perioder: List<OppfolgingPeriodeMinimalDTO>,
        opprettetTidspunkt: ZonedDateTime
    ): OppfolgingPeriodeMinimalDTO? {
        Mockito.`when`(oppfolgingClient.hentOppfolgingsperioder(any()))
            .thenReturn(perioder)
        return oppfolgingsperiodeService.finnOppfolgingsperiode(AKTOR_ID, opprettetTidspunkt.toLocalDateTime())
    }

    private fun oppfperiodeDTO(startDato: ZonedDateTime, sluttDato: ZonedDateTime?): OppfolgingPeriodeMinimalDTO {
        return OppfolgingPeriodeMinimalDTO(UUID.randomUUID(), startDato, sluttDato)
    }
}
