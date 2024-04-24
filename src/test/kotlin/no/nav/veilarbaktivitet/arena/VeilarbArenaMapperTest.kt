package no.nav.veilarbaktivitet.arena

import no.nav.veilarbaktivitet.oppfolging.periode.Oppfolgingsperiode
import no.nav.veilarbaktivitet.testutils.ArenaAktivitetUtils.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class VeilarbArenaMapperTest {

    @Test
    fun `En tiltaksaktivitet har en oppfølgingsperiode hvis sistEndret er innenfor en oppfølgingsperiode`() {
        val oppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusMonths(1), til = ZonedDateTime.now())
        val tiltaksaktivitet = createTiltaksaktivitet().apply { this.statusSistEndret = LocalDate.now().minusDays(1) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(tiltaksaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(oppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En tiltaksaktivitet der sistEndret er mellom to oppfølgingsperioder vil siste oppfølgingsperioden bli satt`() {
        val førsteOppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusYears(2), til = ZonedDateTime.now().minusYears(1))
        val sisteOppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusMonths(2), til = ZonedDateTime.now().minusMonths(1))
        val tiltaksaktivitet = createTiltaksaktivitet().apply { this.statusSistEndret = LocalDate.now().minusMonths(6) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(tiltaksaktivitet, listOf(førsteOppfølgingsperiode, sisteOppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(sisteOppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En tiltaksaktivitet der sistEndret er mellom to oppfølgingsperioder vil siste oppfølgingsperioden bli satt også når den ikke er avsluttet ennå`() {
        val førsteOppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusYears(2), til = ZonedDateTime.now().minusYears(1))
        val sisteOppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusMonths(2), til = null)
        val tiltaksaktivitet = createTiltaksaktivitet().apply { this.statusSistEndret = LocalDate.now().minusMonths(6) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(tiltaksaktivitet, listOf(førsteOppfølgingsperiode, sisteOppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(sisteOppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En tiltaksaktivitet med sistEndret lenge før eneste oppfølgingsperiode skal ikke få oppfølgingsperiode`() {
        val oppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusYears(1), til = null)
        val tiltaksaktivitet = createTiltaksaktivitet().apply { this.statusSistEndret = LocalDate.now().minusYears(6) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(tiltaksaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isNull()
    }

    @Test
    fun `En tiltaksaktivitet med sistEndret rett før eneste oppfølgingsperiode skal få den oppfølgingsperioden`() {
        val oppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusYears(1).minusMinutes(10), til = null)
        val tiltaksaktivitet = createTiltaksaktivitet().apply { this.statusSistEndret = LocalDate.now().minusYears(1) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(tiltaksaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(oppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En utdanningsaktivitet har en oppfølgingsperiode hvis startdato er innenfor en oppfølgingsperiode`() {
        val oppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusMonths(1), til = ZonedDateTime.now())
        val utdanningsaktivitet = createUtdanningsaktivitet().apply { this.aktivitetPeriode.fom = LocalDate.now().minusDays(1) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(utdanningsaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(oppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En utdanningsaktivitet der startDato er mellom to oppfølgingsperioder vil siste oppfølgingsperioden bli satt`() {
        val førsteOppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusYears(2), til = ZonedDateTime.now().minusYears(1))
        val sisteOppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusMonths(2), til = ZonedDateTime.now().minusMonths(1))
        val utdanningsaktivitet = createUtdanningsaktivitet().apply { this.aktivitetPeriode.fom = LocalDate.now().minusMonths(6) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(utdanningsaktivitet, listOf(førsteOppfølgingsperiode, sisteOppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(sisteOppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En utdanningsaktivitet der startDato er mellom to oppfølgingsperioder vil siste oppfølgingsperioden bli satt også når den ikke er avsluttet ennå`() {
        val førsteOppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusYears(2), til = ZonedDateTime.now().minusYears(1))
        val sisteOppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusMonths(2), til = null)
        val utdanningsaktivitet = createUtdanningsaktivitet().apply { this.aktivitetPeriode.fom = LocalDate.now().minusMonths(6) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(utdanningsaktivitet, listOf(førsteOppfølgingsperiode, sisteOppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(sisteOppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En utdanningsaktivitet med startDato lenge før eneste oppfølgingsperiode skal ikke få oppfølgingsperiode`() {
        val oppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusYears(1), til = null)
        val utdanningsaktivitet = createUtdanningsaktivitet().apply { this.aktivitetPeriode.fom = LocalDate.now().minusYears(6) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(utdanningsaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isNull()
    }

    @Test
    fun `En utdanningsaktivitet med startDato rett før eneste oppfølgingsperiode skal få den oppfølgingsperioden`() {
        val oppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusYears(1).minusMinutes(5), til = null)
        val utdanningsaktivitet = createUtdanningsaktivitet().apply { this.aktivitetPeriode.fom = LocalDate.now().minusYears(1) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(utdanningsaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(oppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En gruppeaktivitet har en oppfølgingsperiode hvis startdato på første møte er innenfor en oppfølgingsperiode`() {
        val oppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusMonths(1), til = ZonedDateTime.now())
        val gruppeaktivitet = createGruppeaktivitet().apply { this.moteplanListe[0].startDato = LocalDate.now().minusDays(1) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(gruppeaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(oppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En gruppeaktivitet der startDato på første møte er mellom to oppfølgingsperioder vil siste oppfølgingsperioden bli satt`() {
        val førsteOppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusYears(2), til = ZonedDateTime.now().minusYears(1))
        val sisteOppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusMonths(2), til = ZonedDateTime.now().minusMonths(1))
        val gruppeaktivitet = createGruppeaktivitet().apply { this.moteplanListe[0].startDato = LocalDate.now().minusMonths(6) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(gruppeaktivitet, listOf(førsteOppfølgingsperiode, sisteOppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(sisteOppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En gruppeaktivitet der startDato på første møte er mellom to oppfølgingsperioder vil siste oppfølgingsperioden bli satt også når den ikke er avsluttet ennå`() {
        val førsteOppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusYears(2), til = ZonedDateTime.now().minusYears(1))
        val sisteOppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusMonths(2), til = null)
        val gruppeaktivitet = createGruppeaktivitet().apply { this.moteplanListe[0].startDato = LocalDate.now().minusMonths(6) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(gruppeaktivitet, listOf(førsteOppfølgingsperiode, sisteOppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(sisteOppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En gruppeaktivitet der startDato på første møte lenge er før eneste oppfølgingsperiode skal ikke få oppfølgingsperiode`() {
        val oppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusYears(1), til = null)
        val gruppeaktivitet = createGruppeaktivitet().apply { this.moteplanListe[0].startDato = LocalDate.now().minusYears(6) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(gruppeaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isNull()
    }

    @Test
    fun `En gruppeaktivitet der startDato på første møte er rett før eneste oppfølgingsperiode skal få den oppfølgingsperioden`() {
        val oppfølgingsperiode = oppfølgingsperiode(fra = ZonedDateTime.now().minusYears(1).minusMinutes(5), til = null)
        val gruppeaktivitet = createGruppeaktivitet().apply { this.moteplanListe[0].startDato = LocalDate.now().minusYears(1) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(gruppeaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(oppfølgingsperiode.oppfolgingsperiodeId)
    }

    fun oppfølgingsperiode(fra: ZonedDateTime, til: ZonedDateTime?) = Oppfolgingsperiode("123", UUID.randomUUID(), fra, til)
}