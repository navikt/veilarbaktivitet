package no.nav.veilarbaktivitet.aktivitet

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import no.nav.veilarbaktivitet.util.DateUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class HistorikkServiceTest {

    @Test
    fun `Skal lage historikk av kun én aktivitet-versjon`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet)))

        assertThat(historikk.size).isEqualTo(1)
        assertThat(historikk[aktivitet.id]!!.endringer).hasSize(1)
    }

    @Test
    fun `Skal lage historikk på endret møtetid og sted`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(
            aktivitet,
            Innsender.NAV,
            AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET,
            Date(),
            "Z12345"
        )

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV endret tid eller sted for møtet",
            "${oppdatertAktivitet.endretAv} endret tid eller sted for møtet"
        )
    }

    @Test
    fun `Skal lage historikk på status endret`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val oppdatertAktivitet =
            endreAktivitet(aktivitet, Innsender.NAV, AktivitetTransaksjonsType.STATUS_ENDRET, Date(), "Z12345")

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV flyttet aktiviteten fra ${oppdatertAktivitet.status} til ${oppdatertAktivitet.status}",
            "${oppdatertAktivitet.endretAv} flyttet aktiviteten fra ${oppdatertAktivitet.status} til ${oppdatertAktivitet.status}"
        )
    }

    private fun assert(
        historikk: Historikk,
        oppdatertAktivitet: AktivitetData,
        beskrivelseForBruker: String,
        beskrivelseForVeileder: String
    ) {
        assertThat(historikk.endringer).hasSize(2)
        val endring = historikk.endringer.first()
        assertThat(endring.tidspunkt).isEqualTo(DateUtils.dateToZonedDateTime(oppdatertAktivitet.endretDato))
        assertThat(endring.endretAv).isEqualTo(oppdatertAktivitet.endretAv)
        assertThat(endring.endretAvType).isEqualTo(oppdatertAktivitet.endretAvType)
        assertThat(endring.beskrivelseForBruker).isEqualTo(beskrivelseForBruker)
        assertThat(endring.beskrivelseForVeileder).isEqualTo(beskrivelseForVeileder)
    }

    private fun endreAktivitet(
        aktivitet: AktivitetData,
        endretAvType: Innsender,
        transaksjonsType: AktivitetTransaksjonsType,
        endretDato: Date,
        endretAv: String
    ): AktivitetData {
        return AktivitetData.builder()
            .id(aktivitet.id) // Hvis denne persisteres, vil den få en ny id fra sekvens
            .aktorId(aktivitet.aktorId)
            .versjon(aktivitet.versjon + 1) // Hvis denne persisteres vil den få en ny versjon fra sekvens
            .fraDato(aktivitet.fraDato)
            .tilDato(aktivitet.tilDato)
            .tittel(aktivitet.tittel)
            .beskrivelse(aktivitet.beskrivelse)
            .versjon(aktivitet.versjon + 1)
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

