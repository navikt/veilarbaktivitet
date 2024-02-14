package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

class ArkiveringsutilsTest {

    @Test
    fun `Skal returnere true når en aktivitet er endret etter tidspunktet for forhåndsvisning`() {
        val forhåndsvistTidspunkt = ZonedDateTime.now().minusSeconds(1)
        val aktivitetEndret = Date.from(Instant.now())
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.IJOBB).toBuilder()
            .endretDato(aktivitetEndret).build()

        val resultat = aktiviteterOgDialogerOppdatertEtter(forhåndsvistTidspunkt, listOf(aktivitet), emptyList())

        Assertions.assertThat(resultat).isTrue()
    }

    @Test
    fun `Skal returnere false når en aktivitet ikke er endret etter tidspunktet for forhåndsvisning`() {
        val forhåndsvistTidspunkt = ZonedDateTime.now()
        val aktivitetEndret = Date.from(Instant.now().minusSeconds(1))
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.IJOBB).toBuilder()
            .endretDato(aktivitetEndret).build()

        val resultat = aktiviteterOgDialogerOppdatertEtter(forhåndsvistTidspunkt, listOf(aktivitet), emptyList())

        Assertions.assertThat(resultat).isFalse()
    }
}