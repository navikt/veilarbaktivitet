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
    fun `En tiltaksaktivitet har en oppfølgingsperiode hvis tilOgMedDato er innenfor en oppfølgingsperiode`() {

    }

    @Test
    fun `En tiltaksaktivitet har en oppfølgingsperiode hvis sistEndret er innenfor en oppfølgingsperiode selv om tilOgMedDato er utafor`() {

    }
}