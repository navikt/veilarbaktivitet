package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.stilling_fra_nav.CvKanDelesData
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.sql.Date
import java.time.Instant

class AktivitetDtoArkivPayloadTest {

    @Test
    fun `Møte har riktige felt`() {
        val mote = AktivitetDataTestBuilder.nyMoteAktivitet().toArkivPayload(emptyList(), Historikk(emptyList()))
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
        val mote = AktivitetDataTestBuilder.nySamtaleReferat().toArkivPayload(emptyList(), Historikk(emptyList()))
        assertThat(mote.detaljer.map { it.tittel })
            .containsExactly(
                "Dato",
                "Møteform",
                "Samtalereferat",
            )
    }

    @Test
    fun `Stilling har riktige felt`() {
        val mote = AktivitetDataTestBuilder.nyttStillingssok().toArkivPayload(emptyList(), Historikk(emptyList()))
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
        val mote = AktivitetDataTestBuilder.nyStillingFraNav().toArkivPayload(emptyList(), Historikk(emptyList()))
        assertThat(mote.detaljer.map { it.tittel })
            .contains(
                "Arbeidsgiver",
                "Arbeidssted",
                "Svarfrist",
                "Søknadsfrist",
                "Les mer om stillingen"
            )
    }

    @Test
    fun `Stilling fra NAV lager riktig svartekst når veileder har svart ja`() {
        val mote = AktivitetDataTestBuilder.nyStillingFraNavMedCVKanDeles()
        mote.stillingFraNavData.cvKanDelesData =
            CvKanDelesData.builder()
                .kanDeles(true)
                .endretAvType(Innsender.NAV)
                .endretAv("Z12345")
                .avtaltDato(Date.from(Instant.parse("2022-10-04T13:23:15.321+02:00")))
                .endretTidspunkt(Date.from(Instant.parse("2022-10-06T13:23:15.321+02:00")))
                .build()

        val stillingFraNavDetaljer = mote.toStillingFraNavDetaljer()

        val svarDetalj = stillingFraNavDetaljer.find { it.tittel == "Du svarte at du er interessert" }
        assertThat(svarDetalj!!.tekst).isEqualTo("""
            NAV var i kontakt med deg 4. oktober 2022. Du sa Ja til at CV-en din deles med arbeidsgiver.
  
            NAV svarte på vegne av deg 6. oktober 2022.
  
            Arbeidsgiveren eller NAV vil kontakte deg hvis du er aktuell for stillingen.
        """.trimIndent())
    }

    @Test
    fun `Stilling fra NAV lager riktig svartekst når bruker har svart ja`() {
        val mote = AktivitetDataTestBuilder.nyStillingFraNavMedCVKanDeles()
        mote.stillingFraNavData.cvKanDelesData =
            CvKanDelesData.builder()
                .kanDeles(true)
                .endretAvType(Innsender.BRUKER)
                .endretAv("Bruker Brukersen")
                .avtaltDato(Date.from(Instant.parse("2022-10-04T13:23:15.321+02:00")))
                .endretTidspunkt(Date.from(Instant.parse("2022-10-06T13:23:15.321+02:00")))
                .build()

        val stillingFraNavDetaljer = mote.toStillingFraNavDetaljer()

        val svarDetalj = stillingFraNavDetaljer.find { it.tittel == "Du svarte at du er interessert" }
        assertThat(svarDetalj!!.tekst).isEqualTo("""
            Ja, og NAV kan dele CV-en min med denne arbeidsgiveren.
  
            Du svarte 6. oktober 2022.
  
            Arbeidsgiveren eller NAV vil kontakte deg hvis du er aktuell for stillingen.
        """.trimIndent())
    }


    @Test
    fun `Stilling fra NAV lager riktig svartekst når veileder har svart nei`() {
        val mote = AktivitetDataTestBuilder.nyStillingFraNavMedCVKanDeles()
        mote.stillingFraNavData.cvKanDelesData =
            CvKanDelesData.builder()
                .kanDeles(false)
                .endretAvType(Innsender.NAV)
                .avtaltDato(Date.from(Instant.parse("2022-10-04T13:23:15.321+02:00")))
                .endretTidspunkt(Date.from(Instant.parse("2022-10-06T13:23:15.321+02:00")))
                .build()

        val stillingFraNavDetaljer = mote.toStillingFraNavDetaljer()

        val svarDetalj = stillingFraNavDetaljer.find { it.tittel == "Du svarte at du ikke er interessert" }
        assertThat(svarDetalj!!.tekst).isEqualTo("""
            NAV var i kontakt med deg 4. oktober 2022. Du sa Nei til at CV-en din deles med arbeidsgiver.
  
            NAV svarte på vegne av deg 6. oktober 2022.
        """.trimIndent())
    }

    @Test
    fun `Stilling fra NAV lager riktig svartekst når bruker har svart nei`() {
        val mote = AktivitetDataTestBuilder.nyStillingFraNavMedCVKanDeles()
        mote.stillingFraNavData.cvKanDelesData =
            CvKanDelesData.builder()
                .kanDeles(false)
                .endretAvType(Innsender.BRUKER)
                .endretAv("Bruker Brukersen")
                .avtaltDato(Date.from(Instant.parse("2022-10-04T13:23:15.321+02:00")))
                .endretTidspunkt(Date.from(Instant.parse("2022-10-06T13:23:15.321+02:00")))
                .build()

        val stillingFraNavDetaljer = mote.toStillingFraNavDetaljer()

        val svarDetalj = stillingFraNavDetaljer.find { it.tittel == "Du svarte at du ikke er interessert" }
        assertThat(svarDetalj!!.tekst).isEqualTo("""
            Nei, og jeg vil ikke at NAV skal dele CV-en min med arbeidsgiveren.
  
            Du svarte 6. oktober 2022.
        """.trimIndent())
    }

    @Test
    fun `Jobbrettet egenaktivitet har riktige felt`() {
        val mote = AktivitetDataTestBuilder.nyEgenaktivitet().toArkivPayload(emptyList(), Historikk(emptyList()))
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
        val mote = AktivitetDataTestBuilder.nySokeAvtaleAktivitet().toArkivPayload(emptyList(), Historikk(emptyList()))
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
        val behandling = AktivitetDataTestBuilder.nyBehandlingAktivitet().toArkivPayload(emptyList(), Historikk(emptyList()))
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
            .toArkivPayload(emptyList(), Historikk(emptyList()))
        assertThat(eksternAktivitet.detaljer.map { it.tittel })
            .containsExactly(
                "Fra dato",
                "Til dato",
                "Detail label",
                "Beskrivelse",
            )
    }

    @Test
    fun `IJobb (jobb jeg har) skal ha riktige felt`() {
        val ijobb = AktivitetDataTestBuilder.nyIJobbAktivitet().toArkivPayload(emptyList(), Historikk(emptyList()))
        assertThat(ijobb.detaljer.map { it.tittel })
            .containsExactly(
                "Fra dato",
                "Til dato",
                "Stillingsandel",
                "Arbeidsgiver",
                "Ansettelsesforhold",
                "Beskrivelse",
            )
    }
}
