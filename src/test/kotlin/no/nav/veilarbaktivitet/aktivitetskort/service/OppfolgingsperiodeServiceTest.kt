package no.nav.veilarbaktivitet.aktivitetskort.service

import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.Person.AktorId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

class OppfolgingsperiodeServiceTest {
    private lateinit var oppfolgingClient: OppfolgingV2Client
    private lateinit var oppfolgingsperiodeService: OppfolgingsperiodeService

    @BeforeEach
    fun setup() {
        oppfolgingClient = Mockito.mock(OppfolgingV2Client::class.java)
        oppfolgingsperiodeService = OppfolgingsperiodeService(oppfolgingClient)
    }

    @Test
    fun `opprettetTidspunkt passer i gammel periode`() {
        val riktigPeriode: OppfolgingPeriodeMinimalDTO =
            oppfperiodeDTO(DATE_TIME.minusDays(30), DATE_TIME.minusDays(20))
        val perioder = listOf(
            riktigPeriode,
            oppfperiodeDTO(DATE_TIME.minusDays(10), null)
        )
        val oppfolgingsperiode: OppfolgingPeriodeMinimalDTO? =
            stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(25))
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    @Test
    fun `opprettetTidspunkt passer i gjeldende periode`() {
        val riktigPeriode: OppfolgingPeriodeMinimalDTO = oppfperiodeDTO(DATE_TIME.minusDays(10), null)
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME.minusDays(30), DATE_TIME.minusDays(20)),
            riktigPeriode
        )
        val oppfolgingsperiode: OppfolgingPeriodeMinimalDTO? =
            stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusHours(10))
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    @Test
    fun `opprettetTidspunkt passer paa startDato`() { // skal være inklusiv med andre ord
        val riktigPeriode: OppfolgingPeriodeMinimalDTO = oppfperiodeDTO(DATE_TIME.minusDays(10), null)
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME.minusDays(30), DATE_TIME.minusDays(20)),
            riktigPeriode
        )
        val oppfolgingsperiode: OppfolgingPeriodeMinimalDTO? =
            stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(10))
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    @Test
    fun `feil periode er naermere, men foer oppstart`() {
        val riktigPeriode: OppfolgingPeriodeMinimalDTO = oppfperiodeDTO(DATE_TIME.plusDays(6), null)
        val perioder = listOf(
            riktigPeriode,
            oppfperiodeDTO(DATE_TIME.minusDays(4), DATE_TIME.minusDays(2))
        )

        val oppfolgingsperiode: OppfolgingPeriodeMinimalDTO? =
            stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME)
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    @Test
    fun `opprettetTidspunkti to gamle perioder`() {
        // Er riktig fordi den er "nyere" enn den andre perioden
        val riktigPeriode: OppfolgingPeriodeMinimalDTO = oppfperiodeDTO(DATE_TIME.minusDays(16), DATE_TIME.minusDays(5))
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME.minusDays(20), DATE_TIME.minusDays(10)),
            riktigPeriode
        )
        val oppfolgingsperiode: OppfolgingPeriodeMinimalDTO? =
            stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(15))
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    @Test
    fun `opprettetTidspunkt i en gammel og en gjeldende periode`() {
        val riktigPeriode: OppfolgingPeriodeMinimalDTO = oppfperiodeDTO(DATE_TIME.minusDays(16), DATE_TIME)
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME.minusDays(20), DATE_TIME.minusDays(10)),
            riktigPeriode
        )
        val oppfolgingsperiode: OppfolgingPeriodeMinimalDTO? =
            stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(15))
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    @Test
    fun `opprettetTidspunkt mot en bruker som ikke har oppfolgingsperioder`() {
        val perioder: List<OppfolgingPeriodeMinimalDTO> = listOf()
        val oppfolgingsperiode: OppfolgingPeriodeMinimalDTO? =
            stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(15))
        assertThat(oppfolgingsperiode).isNull()
    }

    @Test
    fun `velg naermeste periode etter opprettetitdspunkt OG som er 10 min innen opprettetTidspunkt`() {
        val riktigPeriode: OppfolgingPeriodeMinimalDTO =
            oppfperiodeDTO(DATE_TIME.minusDays(10).plusMinutes(5), DATE_TIME)
        val perioder = listOf(
            oppfperiodeDTO(DATE_TIME.minusDays(10).minusMinutes(4), DATE_TIME.minusDays(10).minusMinutes(2)),
            riktigPeriode
        )
        val oppfolgingsperiode: OppfolgingPeriodeMinimalDTO? =
            stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(10))
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    @Test
    fun `ikke velg periode hvis perioden slutter foer aktivitetens opprettetTidspunkt`() {
        val riktigPeriode: OppfolgingPeriodeMinimalDTO =
            oppfperiodeDTO(DATE_TIME.minusDays(10).minusMinutes(5), DATE_TIME.minusDays(10).minusMinutes(2))
        val perioder = listOf(
            riktigPeriode
        )
        val oppfolgingsperiode: OppfolgingPeriodeMinimalDTO? =
            stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(10))
        assertThat(oppfolgingsperiode).isNull()
    }

    @Test
    fun ti_min_innen_en_gjeldende_periode() {
        val riktigPeriode: OppfolgingPeriodeMinimalDTO = oppfperiodeDTO(DATE_TIME.minusDays(10).plusMinutes(5), null)
        val perioder = listOf(
            riktigPeriode
        )
        val oppfolgingsperiode: OppfolgingPeriodeMinimalDTO? =
            stubOgFinnOppgolgingsperiode(perioder, LOCAL_DATE_TIME.minusDays(10))
        assertThat(oppfolgingsperiode!!.uuid).isEqualTo(riktigPeriode.uuid)
    }

    private fun stubOgFinnOppgolgingsperiode(
        perioder: List<OppfolgingPeriodeMinimalDTO>,
        opprettetTidspunkt: LocalDateTime
    ): OppfolgingPeriodeMinimalDTO? {
        Mockito.`when`(oppfolgingClient.hentOppfolgingsperioder(any()))
            .thenReturn(perioder)

        return oppfolgingsperiodeService.finnOppfolgingsperiode(AKTORID, opprettetTidspunkt)
    }

    private fun oppfperiodeDTO(startDato: ZonedDateTime, sluttDato: ZonedDateTime?): OppfolgingPeriodeMinimalDTO {
        return OppfolgingPeriodeMinimalDTO(
            UUID.randomUUID(),
            startDato,
            sluttDato
        )
    }

    companion object {
        private val AKTORID: AktorId = Person.aktorId("123")
        private val DATE_TIME = ZonedDateTime.now()
        private val LOCAL_DATE_TIME = DATE_TIME.toLocalDateTime()
    }
}
