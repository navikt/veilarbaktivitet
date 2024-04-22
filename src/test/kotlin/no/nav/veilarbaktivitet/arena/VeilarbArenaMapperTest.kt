package no.nav.veilarbaktivitet.arena

import no.nav.veilarbaktivitet.arena.model.AktiviteterDTO
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
    fun `En utdanningsaktivitet har en oppfølgingsperiode hvis tilOgMed er innenfor en oppfølgingsperiode`() {
        val oppfølgingsperiode = Oppfolgingsperiode(
            "123",
            UUID.randomUUID(),
            ZonedDateTime.now().minusMonths(1),
            ZonedDateTime.now()
        )
        val tiltaksaktivitet = createUtdanningsaktivitet().apply {
            this.aktivitetPeriode.tom = LocalDate.now().minusDays(1)
        }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(tiltaksaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(oppfølgingsperiode.oppfolgingsperiodeId)
    }

    @Test
    fun `En gruppeaktivitet har en oppfølgingsperiode hvis tilOgMed på siste møteplan er innenfor en oppfølgingsperiode`() {
        val oppfølgingsperiode = Oppfolgingsperiode(
            "123",
            UUID.randomUUID(),
            ZonedDateTime.now().minusMonths(1),
            ZonedDateTime.now()
        )
        val tiltaksaktivitet = createGruppeaktivitet().apply {
            this.moteplanListe = listOf(
                AktiviteterDTO.Gruppeaktivitet.Moteplan().apply {
                    this.startDato = LocalDate.now().minusYears(2)
                    this.sluttDato = LocalDate.now().minusYears(1)
                },
                AktiviteterDTO.Gruppeaktivitet.Moteplan().apply {
                    this.startDato = LocalDate.now().minusMonths(2)
                    this.sluttDato = LocalDate.now().minusDays(1)
                }
            )
        }

        val mappetAktivitet = VeilarbarenaMapper.mapTilAktivitet(tiltaksaktivitet, listOf(oppfølgingsperiode))

        assertThat(mappetAktivitet.oppfolgingsperiodeId).isEqualTo(oppfølgingsperiode.oppfolgingsperiodeId)
    }


    @Test
    fun `En aktivitet har en oppfølgingsperiode hvis sistEndret er innenfor en oppfølgingsperiode selv om tilOgMedDato er utafor`() {
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
    fun `En aktivitet tilhører oppfølgingsperioden som sistEndret er innenfor, og ikke den tilOgMedDato er innenfor`() {
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