package no.nav.veilarbaktivitet.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FødselsnummerTypeTest {

    @Test
    fun `Skal kunne skrive fødselsnumre fra Dolly som ordinært fnr`() {
        val fnr = "01418712345"
        val ordinærFnr = tilOrdinærtFødselsnummerFormat(fnr)
        assertThat(ordinærFnr).isEqualTo("01018712345")
    }

    @Test
    fun `Skal kunne skrive fødselsnumre fra TestNorge som ordinært fnr`() {
        val fnr = "01818712345"
        val ordinærFnr = tilOrdinærtFødselsnummerFormat(fnr)
        assertThat(ordinærFnr).isEqualTo("01018712345")
    }

    @Test
    fun `Skal kunne skrive D-numre som ordinært fnr`() {
        val dnr = "41118712345"
        val ordinærFnr = tilOrdinærtFødselsnummerFormat(dnr)
        assertThat(ordinærFnr).isEqualTo("01118712345")
    }

    @Test
    fun `Skal ikke endre ordinære fødselsnumre`() {
        val fnr = "01118712345"
        val ordinærFnr = tilOrdinærtFødselsnummerFormat(fnr)
        assertThat(ordinærFnr).isEqualTo("01118712345")
    }
}