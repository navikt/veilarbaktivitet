package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AktivitetDtoArkivPayloadTest {

    @Test
    fun `møte og samtalereferat skal ikke ha beskrivelse 2 ganger`() {
        val mote = AktivitetDataTestBuilder.nyMoteAktivitet().toArkivPayload()
        assertThat(mote.detaljer.map { it.tittel })
            .containsExactly("Beskrivelse","Møteform",
                "Er publisert",
                "Møtested eller annen praktisk informasjon",
                "Hensikt med møtet",
                "Dato",
                "Forberedelser til møtet",
                "Lenke")

//        assertThat(mote.detaljer.map { it.tittel }).doesNotContain("Beskrivelse")
//        assertThat(mote.detaljer.filter { it.tittel == "Hensikt med møtet" }).isNotEmpty()
//        val samtale = AktivitetDataTestBuilder.nySamtaleReferat().toArkivPayload()
//        assertThat(samtale.detaljer.filter { it.tittel == "Beskrivelse" }).isEmpty()
//        assertThat(samtale.detaljer.filter { it.tittel == "Hensikt med møtet" }).isNotEmpty()
//        val egen = AktivitetDataTestBuilder.nyEgenaktivitet().toArkivPayload()
//        assertThat(egen.detaljer.filter { it.tittel == "Beskrivelse" }).isNotEmpty()
//        assertThat(egen.detaljer.filter { it.tittel == "Hensikt med møtet" }).isEmpty()
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