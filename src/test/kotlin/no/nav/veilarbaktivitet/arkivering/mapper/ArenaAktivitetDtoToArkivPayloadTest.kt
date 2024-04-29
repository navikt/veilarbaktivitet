package no.nav.veilarbaktivitet.arkivering.mapper

import no.nav.veilarbaktivitet.arena.model.AktiviteterDTO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaStatusDTO
import no.nav.veilarbaktivitet.arena.model.ArenaStatusDTO.*
import no.nav.veilarbaktivitet.arena.model.MoteplanDTO
import no.nav.veilarbaktivitet.arkivering.Detalj
import no.nav.veilarbaktivitet.arkivering.Stil
import no.nav.veilarbaktivitet.arkivering.etiketter.ArkivEtikett
import no.nav.veilarbaktivitet.arkivering.etiketter.ArkivEtikettStil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.sql.Date
import java.time.LocalDate
import java.time.Month

class ArenaAktivitetDtoToArkivPayloadTest {

    @Test
    fun `Skal mappe avtaltMedNav til riktig etikett`() {
        val avtaltAktivitet = ArenaAktivitetDTO().setAvtalt(true)
        assertThat(avtaltAktivitet.toArkivEtikett()).containsExactly(ArkivEtikett(ArkivEtikettStil.AVTALT,"Avtalt med NAV"))
        val ikkeAvtaltAktivitet = ArenaAktivitetDTO().setAvtalt(false)
        assertThat(ikkeAvtaltAktivitet.toArkivEtikett()).isEmpty()
    }

    @Test
    fun `Skal mappe arena-etikett til riktig arkiv-etikett`() {
        val aktuellAktivitet = ArenaAktivitetDTO().setEtikett(AKTUELL)
        val avslagAktivitet = ArenaAktivitetDTO().setEtikett(AVSLAG)
        val ikkeAktuellAktivitet = ArenaAktivitetDTO().setEtikett(IKKAKTUELL)
        val ikkeMøttAktivitet = ArenaAktivitetDTO().setEtikett(IKKEM)
        val infomøteAktivitet = ArenaAktivitetDTO().setEtikett(INFOMOETE)
        val jaTakkAktivitet = ArenaAktivitetDTO().setEtikett(JATAKK)
        val neiTakkAktivitet = ArenaAktivitetDTO().setEtikett(NEITAKK)
        val tilbudAktivitet = ArenaAktivitetDTO().setEtikett(TILBUD)
        val ventelisteAktivitet = ArenaAktivitetDTO().setEtikett(VENTELISTE)
        val utenEtikettAktivitet = ArenaAktivitetDTO().setEtikett(null)

        assertThat(aktuellAktivitet.toArkivEtikett()).containsExactly(ArkivEtikett(ArkivEtikettStil.POSITIVE, "Søkt inn på tiltaket"))
        assertThat(avslagAktivitet.toArkivEtikett()).containsExactly(ArkivEtikett(ArkivEtikettStil.NEGATIVE, "Fått avslag"))
        assertThat(ikkeAktuellAktivitet.toArkivEtikett()).containsExactly(ArkivEtikett(ArkivEtikettStil.NEUTRAL, "Ikke aktuell for tiltaket"))
        assertThat(ikkeMøttAktivitet.toArkivEtikett()).containsExactly(ArkivEtikett(ArkivEtikettStil.NEGATIVE, "Ikke møtt på tiltaket"))
        assertThat(infomøteAktivitet.toArkivEtikett()).containsExactly(ArkivEtikett(ArkivEtikettStil.POSITIVE, "Infomøte før tiltaket"))
        assertThat(jaTakkAktivitet.toArkivEtikett()).containsExactly(ArkivEtikett(ArkivEtikettStil.POSITIVE, "Takket ja til tilbud"))
        assertThat(neiTakkAktivitet.toArkivEtikett()).containsExactly(ArkivEtikett(ArkivEtikettStil.NEUTRAL, "Takket nei til tilbud"))
        assertThat(tilbudAktivitet.toArkivEtikett()).containsExactly(ArkivEtikett(ArkivEtikettStil.POSITIVE, "Fått plass på tiltaket"))
        assertThat(ventelisteAktivitet.toArkivEtikett()).containsExactly(ArkivEtikett(ArkivEtikettStil.POSITIVE, "På venteliste"))
        assertThat(utenEtikettAktivitet.toArkivEtikett()).isEmpty()
    }

    @Test
    fun `Skal mappe gruppeaktivitetdetaljer riktig`() {
        val møteplaner = listOf(
            MoteplanDTO().apply {
                this.startDato = Date.valueOf(LocalDate.of(2022, Month.JANUARY, 1))
                this.sluttDato = Date.valueOf(LocalDate.of(2022, Month.JANUARY, 2))
                this.sted = "Oslo"
            },
            MoteplanDTO().apply {
                this.startDato = Date.valueOf(LocalDate.of(2022, Month.JANUARY, 1))
                this.sluttDato = Date.valueOf(LocalDate.of(2022, Month.JANUARY, 1))
                this.sted = "Oslo"
            },
            MoteplanDTO().apply {
                this.startDato = Date.valueOf(LocalDate.of(2022, Month.JANUARY, 1))
                this.sluttDato = null
                this.sted = "Oslo"
            }
        )
        val aktivitetMedMøteplan = ArenaAktivitetDTO().apply { moeteplanListe = møteplaner }

        val gruppeaktivitetDetaljer = aktivitetMedMøteplan.toGruppeaktivitetDetaljer()

        assertThat(gruppeaktivitetDetaljer).containsExactly(Detalj(Stil.PARAGRAF, "Tidspunkt og sted", tekst = ""))
    }

    /*

fun ArenaAktivitetDTO.toGruppeaktivitetDetaljer(): List<Detalj> {
    val møteplanTekst = this.moeteplanListe.map {
        if(it.sluttDato.equals(it.startDato) ) {
            " - ${norskDato(it.startDato)} - ${norskDato(it.sluttDato)} ${it.sted}"
    } else {
            " - ${norskDato(it.startDato)} ${it.sted}"
        }
    }.joinToString(separator = "\n")
    return listOf(
        Detalj(Stil.PARAGRAF, "Tidspunkt og sted", tekst = møteplanTekst)
    )
}
     */

}