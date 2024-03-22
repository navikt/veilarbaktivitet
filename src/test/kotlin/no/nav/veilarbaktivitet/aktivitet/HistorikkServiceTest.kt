package no.nav.veilarbaktivitet.aktivitet

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class HistorikkServiceTest {

    @Test
    fun `skal lage historikk på endret møtetid og sted`(){
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, Innsender.NAV, AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET, Date(), "Z12345")

        val historikk = lagHistorikk(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assertThat(historikk.size).isEqualTo(1)
        val historikkPåAktiviteten = historikk[aktivitet.id]!!
        assertThat(historikkPåAktiviteten.endringer).hasSize(1)
        val endring = historikkPåAktiviteten.endringer.first()
        assertThat(endring.endretAv).isEqualTo(oppdatertAktivitet.endretAv)
        assertThat(endring.endretAvType).isEqualTo(oppdatertAktivitet.endretAvType)
        assertThat(endring.tidspunkt).isEqualTo(oppdatertAktivitet.endretDato)
        assertThat(endring.beskrivelseForBruker).isEqualTo("NAV endret tid eller sted for møtet")
        assertThat(endring.beskrivelseForVeileder).isEqualTo("NAV endret tid eller sted for møtet")
    }

    private fun endreAktivitet(aktivitet: AktivitetData, endretAvType: Innsender, transaksjonsType: AktivitetTransaksjonsType, endretDato: Date, endretAv: String): AktivitetData {
        return AktivitetData.builder()
            .id(aktivitet.id) // Hvis denne persisteres, vil den få en ny id fra sekvens
            .aktorId(aktivitet.aktorId)
            .versjon(aktivitet.versjon +1) // Hvis denne persisteres vil den få en ny versjon fra sekvens
            .fraDato(aktivitet.fraDato)
            .tilDato(aktivitet.tilDato)
            .tittel(aktivitet.tittel)
            .beskrivelse(aktivitet.beskrivelse)
            .versjon(aktivitet.versjon +1)
            .status(aktivitet.status)
            .avsluttetKommentar(aktivitet.avsluttetKommentar)
            .endretAvType(endretAvType)
            .opprettetDato(aktivitet.opprettetDato)
            .lenke(aktivitet.lenke)
            .transaksjonsType(transaksjonsType)
            .lestAvBrukerForsteGang(aktivitet.lestAvBrukerForsteGang)
            .historiskDato(aktivitet.historiskDato)
            .endretDato(endretDato)
            .endretAv(endretAv)
            .malid("2").build()
    }
}

