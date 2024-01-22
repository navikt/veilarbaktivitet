package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AktivitetDtoArkivPayloadTest {

    @Test
    fun test() {
        val arkivPayload = AktivitetDataTestBuilder.nyMoteAktivitet().toArkivPayload()
        assertThat(arkivPayload.detaljer.filter { it.tittel == "Beskrivelse" }).isEmpty()
    }

}