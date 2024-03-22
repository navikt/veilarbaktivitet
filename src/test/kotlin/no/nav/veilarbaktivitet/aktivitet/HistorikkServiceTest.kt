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
        val oppdatertAktivitet = endreAktivitet(aktivitet, AktivitetTransaksjonsType.MOTE_TID_OG_STED_ENDRET)

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
        val oppdatertAktivitet = endreAktivitet(aktivitet, AktivitetTransaksjonsType.STATUS_ENDRET)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV flyttet aktiviteten fra ${oppdatertAktivitet.status} til ${oppdatertAktivitet.status}",
            "${oppdatertAktivitet.endretAv} flyttet aktiviteten fra ${oppdatertAktivitet.status} til ${oppdatertAktivitet.status}"
        )
    }

    @Test
    fun `Skal lage historikk på opprettet referat på møte`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, AktivitetTransaksjonsType.REFERAT_OPPRETTET)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV opprettet referat",
            "${oppdatertAktivitet.endretAv} opprettet referat"
        )
    }

    @Test
    fun `Skal lage historikk på referat endret`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, AktivitetTransaksjonsType.REFERAT_ENDRET)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV endret referatet",
            "${oppdatertAktivitet.endretAv} endret referatet"
        )
    }

    @Test
    fun `Skal lage historikk på referat publisert`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, AktivitetTransaksjonsType.REFERAT_PUBLISERT)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV delte referatet",
            "${oppdatertAktivitet.endretAv} delte referatet"
        )
    }

    @Test
    fun `Skal lage historikk på at aktivitet ble historisk`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, AktivitetTransaksjonsType.BLE_HISTORISK)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "Aktiviteten ble automatisk arkivert",
            "Aktiviteten ble automatisk arkivert"
        )
    }

    @Test
    fun `Skal lage historikk på at detaljer ble endret`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, AktivitetTransaksjonsType.DETALJER_ENDRET)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV endret detaljer på aktiviteten",
            "${oppdatertAktivitet.endretAv} endret detaljer på aktiviteten"
        )
    }

    @Test
    fun `Skal lage historikk på at aktivitet NAV ble opprettet`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, AktivitetTransaksjonsType.OPPRETTET)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV opprettet aktiviteten",
            "${oppdatertAktivitet.endretAv} opprettet aktiviteten"
        )
    }

    @Test
    fun `Skal lage historikk på at aktivitet avtalt med NAV ble opprettet`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, AktivitetTransaksjonsType.OPPRETTET, avtaltMedNav = true)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV opprettet aktiviteten. Den er automatisk merket som \"Avtalt med NAV\"",
            "${oppdatertAktivitet.endretAv} opprettet aktiviteten. Den er automatisk merket som \"Avtalt med NAV\""
        )
    }

    @Test
    fun `Skal lage historikk på at aktivitet ble avtalt med NAV`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, AktivitetTransaksjonsType.AVTALT, avtaltMedNav = true)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV merket aktiviteten som \"Avtalt med NAV\"",
            "${oppdatertAktivitet.endretAv} merket aktiviteten som \"Avtalt med NAV\""
        )
    }

    @Test
    fun `Skal lage historikk på at aktivitet ble avtalt med NAV når forrige aktivitet også var avtalt`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().avtalt(true).build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, AktivitetTransaksjonsType.AVTALT, avtaltMedNav = true)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV sendte forhåndsorientering",
            "${oppdatertAktivitet.endretAv} sendte forhåndsorientering"
        )
    }

    @Test
    fun `Skal lage historikk på forhåndsorientering lest`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().avtalt(true).build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST, endretAvType = Innsender.BRUKER, endretAv = "Per Persen")

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "Du bekreftet å ha lest informasjon om ansvaret ditt",
            "Bruker bekreftet å ha lest informasjon om ansvaret sitt"
        )
    }

    @Test
    fun `Skal lage historikk på svar på spørsmål om deling av CV`() {
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.MOTE).toBuilder().avtalt(true).build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, AktivitetTransaksjonsType.DEL_CV_SVART, endretAvType = Innsender.BRUKER)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "Du svarte 'Nei' på spørsmålet \"Er du interessert i denne stillingen?\"",
            "Bruker svarte 'Nei' på spørsmålet \"Er du interessert i denne stillingen?\""
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
        transaksjonsType: AktivitetTransaksjonsType,
        endretAvType: Innsender = Innsender.NAV,
        endretDato: Date = Date(),
        endretAv: String = "Z12345",
        avtaltMedNav: Boolean = false
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
            .avtalt(avtaltMedNav)
            .malid("2").build()
    }
}

