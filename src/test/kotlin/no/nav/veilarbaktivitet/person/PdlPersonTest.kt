package no.nav.veilarbaktivitet.person

import no.nav.veilarbaktivitet.person.NavnMaster.FREG
import no.nav.veilarbaktivitet.person.NavnMaster.PDL
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PdlPersonTest {

    @Test
    fun `Skal bruke navn med PDL som master når navn fra både PDL og FREG finnes`() {
        val pdlNavn = PdlNavn("Ole", null, "Johansen", metadata = NavnMetadata(master = PDL.name))
        val fregNav = PdlNavn("Jens", null, "Olsen", metadata = NavnMetadata(master = FREG.name))
        val person = PdlPerson(listOf(pdlNavn, fregNav).shuffled())

        val navn = person.hentNavn()

        assertThat(navn.fornavn).isEqualTo(pdlNavn.fornavn)
        assertThat(navn.etternavn).isEqualTo(pdlNavn.etternavn)
    }

    @Test
    fun `Skal bruke navn med FREG som master når det ikke finnes navn med PDL som master`() {
        val fregNav = PdlNavn("Jens", null, "Olsen", metadata = NavnMetadata(master = FREG.name))
        val person = PdlPerson(listOf(fregNav).shuffled())

        val navn = person.hentNavn()

        assertThat(navn.fornavn).isEqualTo(fregNav.fornavn)
        assertThat(navn.etternavn).isEqualTo(fregNav.etternavn)
    }

    @Test
    fun `Kast exception hvis navn fra hverken PDL og FREG ikke finnes`() {
        val person = PdlPerson(emptyList())

        assertThrows<IllegalStateException> {
            person.hentNavn()
        }
    }
}