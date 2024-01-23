package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AktivitetDtoArkivPayloadTest {

    @Test
    fun `Møte har riktige felt`() {
        val mote = AktivitetDataTestBuilder.nyMoteAktivitet().toArkivPayload()
        assertThat(mote.detaljer.map { it.tittel })
            .containsExactly(
                "Dato",
                "Klokkeslett",
                "Møteform",
                "Varighet",
                "Møtested eller annen praktisk informasjon",
                "Hensikt med møtet",
                "Forberedelser til møtet",
                "Samtalereferat"
            )
    }

    @Test
    fun `Samtalereferat har riktige felt`() {
        val mote = AktivitetDataTestBuilder.nySamtaleReferat().toArkivPayload()
        assertThat(mote.detaljer.map { it.tittel })
            .containsExactly(
                "Dato",
                "Møteform",
                "Samtalereferat",
            )
    }

    @Test
    fun `stilling skal ikke ha tilDato men ha frist`() {
        val mote = AktivitetDataTestBuilder.nyttStillingssok().toArkivPayload()
        assertThat(mote.detaljer.filter { it.tittel == "Til dato" }).isEmpty()
        assertThat(mote.detaljer.filter { it.tittel == "Frist" }).isNotEmpty()
        val egen = AktivitetDataTestBuilder.nyEgenaktivitet().toArkivPayload()
        assertThat(egen.detaljer.filter { it.tittel == "Til dato" }).isNotEmpty()
        assertThat(egen.detaljer.filter { it.tittel == "Frist" }).isEmpty()
    }

    @Test
    fun `møte og samtalereferat skal ikke ha Fra dato men ha Dato`() {
        val samtale = AktivitetDataTestBuilder.nySamtaleReferat().toArkivPayload()
        assertThat(samtale.detaljer.filter { it.tittel == "Til dato" }).isEmpty()
        assertThat(samtale.detaljer.filter { it.tittel == "Dato" }).isNotEmpty()
        val mote = AktivitetDataTestBuilder.nyMoteAktivitet().toArkivPayload()
        assertThat(mote.detaljer.filter { it.tittel == "Til dato" }).isEmpty()
        assertThat(mote.detaljer.filter { it.tittel == "Dato" }).isNotEmpty()
    }

}