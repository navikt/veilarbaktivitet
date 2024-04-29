package no.nav.veilarbaktivitet.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NumberUtilsTest {

    @Test
    fun `Fjern desimaler hvis desimalene er null`() {
        val tall = 1.00F
        val somStreng = tall.toStringWithoutNullDecimals()
        assertThat(somStreng).isEqualTo("1")
    }

    @Test
    fun `Ikke fjern desimaler hvis de er noe annet enn null`() {
        val tall = 1.00005F
        val somStreng = tall.toStringWithoutNullDecimals()
        assertThat(somStreng).isEqualTo("1.00005")
    }

    @Test
    fun `Skal fungere selv om tall er null`() {
        val tall = null
        val somStreng = tall.toStringWithoutNullDecimals()
        assertThat(somStreng).isNull()
    }
}