package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt
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
    fun `Stilling har riktige felt`() {
        val mote = AktivitetDataTestBuilder.nyttStillingssok().toArkivPayload()
        assertThat(mote.detaljer.map { it.tittel })
            .containsExactly(
                "Fra dato",
                "Frist",
                "Arbeidsgiver",
                "Kontaktperson",
                "Arbeidssted",
                "Beskrivelse",
                "Lenke til stillingsannonsen"
            )
    }

    @Test
    fun `Stilling fra NAV har riktige felt`() {
        val mote = AktivitetDataTestBuilder.nyStillingFraNav().toArkivPayload()
        assertThat(mote.detaljer.map { it.tittel })
            .containsExactly(
                "Arbeidsgiver",
                "Arbeidssted",
                "Svarfrist",
                "Søknadsfrist",
                "Les mer om stillingen"
            )
    }

    @Test
    fun `Jobbrettet egenaktivitet har riktige felt`() {
        val mote = AktivitetDataTestBuilder.nyEgenaktivitet().toArkivPayload()
        assertThat(mote.detaljer.map { it.tittel })
            .containsExactly(
                "Fra dato",
                "Til dato",
                "Mål med aktiviteten",
                "Min huskeliste",
                "Beskrivelse",
                "Lenke"
            )
    }

    @Test
    fun `Jobbsøking (sokeavtale) egenaktivitet har riktige felt`() {
        val mote = AktivitetDataTestBuilder.nySokeAvtaleAktivitet().toArkivPayload()
        assertThat(mote.detaljer.map { it.tittel })
            .containsExactly(
                "Fra dato",
                "Til dato",
                "Antall søknader i perioden",
                "Antall søknader i uka",
                "Beskrivelse",
            )
    }

    @Test
    fun `Behandling skal ha riktige felt`() {
        val behandling = AktivitetDataTestBuilder.nyBehandlingAktivitet().toArkivPayload()
        assertThat(behandling.detaljer.map { it.tittel })
            .containsExactly(
                "Type behandling",
                "Behandlingssted",
                "Fra dato",
                "Til dato",
                "Mål for behandlingen",
                "Oppfølging fra NAV",
                "Beskrivelse",
            )
    }

    @Test
    fun `Eksternaktivitet skal ha riktige felt`() {
        val eksternAktivitet = AktivitetDataTestBuilder
            .nyEksternAktivitet()
            .let { it.withEksternAktivitetData(it.eksternAktivitetData.copy(
                detaljer = listOf(
                    Attributt("Detail label", "Detail value")
                ),
            )) }
            .toArkivPayload()
        assertThat(eksternAktivitet.detaljer.map { it.tittel })
            .containsExactly(
                "Fra dato",
                "Til dato",
                "Detail label",
                "Beskrivelse",
            )
    }

//    @Test
//    fun `stilling skal ikke ha tilDato men ha frist`() {
//        val mote = AktivitetDataTestBuilder.nyttStillingssok().toArkivPayload()
//        assertThat(mote.detaljer.filter { it.tittel == "Til dato" }).isEmpty()
//        assertThat(mote.detaljer.filter { it.tittel == "Frist" }).isNotEmpty()
//        val egen = AktivitetDataTestBuilder.nyEgenaktivitet().toArkivPayload()
//        assertThat(egen.detaljer.filter { it.tittel == "Til dato" }).isNotEmpty()
//        assertThat(egen.detaljer.filter { it.tittel == "Frist" }).isEmpty()
//    }
//
//    @Test
//    fun `møte og samtalereferat skal ikke ha Fra dato men ha Dato`() {
//        val samtale = AktivitetDataTestBuilder.nySamtaleReferat().toArkivPayload()
//        assertThat(samtale.detaljer.filter { it.tittel == "Til dato" }).isEmpty()
//        assertThat(samtale.detaljer.filter { it.tittel == "Dato" }).isNotEmpty()
//        val mote = AktivitetDataTestBuilder.nyMoteAktivitet().toArkivPayload()
//        assertThat(mote.detaljer.filter { it.tittel == "Til dato" }).isEmpty()
//        assertThat(mote.detaljer.filter { it.tittel == "Dato" }).isNotEmpty()
//    }

}