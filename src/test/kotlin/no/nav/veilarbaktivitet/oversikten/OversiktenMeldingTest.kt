package no.nav.veilarbaktivitet.oversikten

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class OversiktenMeldingTest {

    @Test
    fun `Melding for udelt samtalereferat skal ha riktige verdier`() {
        val fnr = "1234567891234"
        val melding = OversiktenMelding.forUdeltSamtalereferat(fnr = fnr, operasjon = OversiktenMelding.Operasjon.START, erProd = false)
        assertThat(melding.kategori).isEqualTo(OversiktenMelding.Kategori.UDELT_SAMTALEREFERAT)
        assertThat(melding.avsender).isEqualTo("veilarbaktivitet")
        assertThat(melding.personID).isEqualTo(fnr)
        assertThat(melding.operasjon).isEqualTo(OversiktenMelding.Operasjon.START)
        assertThat(melding.hendelse.beskrivelse).isEqualTo("Bruker har et udelt samtalereferat")
        assertThat(melding.hendelse.lenke).isEqualTo("https://veilarbpersonflate.ansatt.dev.nav.no/aktivitetsplan")
        assertThat(melding.hendelse.dato).isCloseTo(LocalDateTime.now(), within(100, ChronoUnit.MILLIS))
    }

    @Test
    fun `Melding om udelt samtalereferat skal ha riktig URL for prod`() {
        val fnr = "1234567891234"
        val melding = OversiktenMelding.forUdeltSamtalereferat(fnr = fnr, operasjon = OversiktenMelding.Operasjon.START, erProd = true)
        assertThat(melding.hendelse.lenke).isEqualTo("https://veilarbpersonflate.intern.nav.no/aktivitetsplan")
    }

    @Test
    fun `Stoppmelding for udelt samtalereferat skal ha riktig URL for prod`() {
        val fnr = "1234567891234"
        val melding = OversiktenMelding.forUdeltSamtalereferat(fnr = fnr, operasjon = OversiktenMelding.Operasjon.STOPP, erProd = true)
        assertThat(melding.hendelse.lenke).isEqualTo("https://veilarbpersonflate.intern.nav.no/aktivitetsplan")
        assertThat(melding.operasjon).isEqualTo(OversiktenMelding.Operasjon.STOPP)
    }
}