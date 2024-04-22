package no.nav.veilarbaktivitet.arena

import no.nav.veilarbaktivitet.arena.model.AktiviteterDTO
import no.nav.veilarbaktivitet.internapi.InternAktivitetMapper.mapTilAktivitet
import no.nav.veilarbaktivitet.oppfolging.periode.Oppfolgingsperiode
import no.nav.veilarbaktivitet.testutils.ArenaAktivitetUtils.createTiltaksaktivitet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

class VeilarbArenaMapperTest {
    @Test
    fun `En tiltaksaktivitet har en oppfølgingsperiode hvis sistEndret er innenfor en oppfølgingsperiode`() {
        val oppfølgingsperiode = Oppfolgingsperiode(
            "123",
            UUID.randomUUID(),
            ZonedDateTime.now().minusMonths(1),
            ZonedDateTime.now()
        )
        val tiltaksaktivitet = createTiltaksaktivitet().apply { this.statusSistEndret = LocalDate.now().minusDays(1) }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(tiltaksaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(oppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En tiltaksaktivitet har en oppfølgingsperiode hvis tilOgMedDato er innenfor en oppfølgingsperiode når sistEndret er utenfor`() {
        val oppfølgingsperiode = Oppfolgingsperiode(
            "123",
            UUID.randomUUID(),
            ZonedDateTime.now().minusMonths(1),
            ZonedDateTime.now()
        )
        val tiltaksaktivitet = createTiltaksaktivitet().apply {
            this.statusSistEndret = LocalDate.now().minusMonths(2)
            this.deltakelsePeriode.tom = LocalDate.now().minusDays(1)}

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(tiltaksaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(oppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En tiltaksaktivitet har en oppfølgingsperiode hvis sistEndret er innenfor en oppfølgingsperiode selv om tilOgMedDato er utafor`() {
        val oppfølgingsperiode = Oppfolgingsperiode(
            "123",
            UUID.randomUUID(),
            ZonedDateTime.now().minusMonths(1),
            ZonedDateTime.now()
        )
        val tiltaksaktivitet = createTiltaksaktivitet().apply {
            this.statusSistEndret = LocalDate.now().minusDays(1)
            this.deltakelsePeriode.tom = LocalDate.now().plusMonths(1)
        }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(tiltaksaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(oppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En tiltaksaktivitet tilhører oppfølgingsperioden som sistEndret er innenfor, og ikke den tilOgMedDato er innenfor`() {
        val nyOppfølgingsperiode = Oppfolgingsperiode(
            "123",
            UUID.randomUUID(),
            ZonedDateTime.now().minusMonths(1),
            ZonedDateTime.now()
        )
        val gammelOppfølgingsperiode = Oppfolgingsperiode(
            "123",
            UUID.randomUUID(),
            ZonedDateTime.now().minusMonths(3),
            ZonedDateTime.now().minusMonths(5)
        )

        val tiltaksaktivitet = createTiltaksaktivitet().apply {
            this.statusSistEndret = LocalDate.now().minusDays(1)
            this.deltakelsePeriode.tom = LocalDate.now().minusMonths(4)
        }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(tiltaksaktivitet, listOf(nyOppfølgingsperiode, gammelOppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(nyOppfølgingsperiode.oppfolgingsperiodeId)
    }
}