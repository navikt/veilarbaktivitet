package no.nav.veilarbaktivitet.aktivitet

import no.nav.veilarbaktivitet.aktivitet.domain.*
import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekEtikettData.INNKALT_TIL_INTERVJU
import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekEtikettData.SOKNAD_SENDT
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering
import no.nav.veilarbaktivitet.avtalt_med_nav.Type
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.stilling_fra_nav.CvKanDelesData
import no.nav.veilarbaktivitet.stilling_fra_nav.Soknadsstatus
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder.*
import no.nav.veilarbaktivitet.testutils.AktivitetTypeDataTestBuilder
import no.nav.veilarbaktivitet.util.DateUtils
import no.nav.veilarbaktivitet.util.DateUtils.zonedDateTimeToDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class HistorikkServiceTest {

    @Test
    fun `Skal lage historikk på endret møtetid`() {
        val aktivitet = nyAktivitet(AktivitetTypeData.MOTE).toBuilder()
            .fraDato(Date.from(Instant.parse("2022-09-02T11:00:00Z")))
            .tilDato(Date.from(Instant.parse("2022-09-02T12:00:00Z"))).build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, oppdatertMoteData = aktivitet.moteData, fraDato = Date.from(Instant.parse("2022-09-02T12:00:00Z")), tilDato = Date.from(Instant.parse("2022-09-02T13:00:00Z")))

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV endret tid for møtet fra 2. september 2022 kl. 14.00 til 2. september 2022 kl. 15.00",
            "${oppdatertAktivitet.endretAv} endret tid for møtet fra 2. september 2022 kl. 14.00 til 2. september 2022 kl. 15.00"
        )
    }

    @Test
    fun `Skal lage historikk på endret møtested`() {
        val aktivitet = nyAktivitet(AktivitetTypeData.MOTE).toBuilder().moteData(nyMoteAktivitet().moteData.withAdresse("Gammel Adresse 1")).build()
        val oppdatertMoteData = aktivitet.moteData.withAdresse("Ny Adresse 1")
        val oppdatertAktivitet = endreAktivitet(aktivitet, oppdatertMoteData = oppdatertMoteData)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV endret sted for møtet fra Gammel Adresse 1 til Ny Adresse 1",
            "${oppdatertAktivitet.endretAv} endret sted for møtet fra Gammel Adresse 1 til Ny Adresse 1"
        )
    }


    @Test
    fun `Skal lage historikk på endret kanal for møte`() {
        val aktivitet = nyAktivitet(AktivitetTypeData.MOTE).toBuilder().moteData(nyMoteAktivitet().moteData.withKanal(KanalDTO.OPPMOTE)).build()
        val oppdatertMoteData = aktivitet.moteData.withKanal(KanalDTO.TELEFON)
        val oppdatertAktivitet = endreAktivitet(aktivitet, oppdatertMoteData = oppdatertMoteData)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV endret kanal for møtet fra møte til telefonmøte",
            "${oppdatertAktivitet.endretAv} endret kanal for møtet fra møte til telefonmøte"
        )
    }

    @Test
    fun `Skal lage historikk på status endret`() {
        val aktivitet = nyAktivitet(AktivitetTypeData.EGENAKTIVITET).toBuilder().build().withStatus(AktivitetStatus.GJENNOMFORES)
        val oppdatertAktivitet = endreAktivitet(aktivitet, oppdatertStatus = AktivitetStatus.FULLFORT)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV flyttet aktiviteten fra Gjennomføres til Fullført",
            "${oppdatertAktivitet.endretAv} flyttet aktiviteten fra Gjennomføres til Fullført"
        )
    }

    @Test
    fun `Skal lage historikk på opprettet referat på møte`() {
        val moteDataUtenReferat = nyMoteAktivitet().moteData.withReferat("")
        val aktivitet = nyMoteAktivitet().withMoteData(moteDataUtenReferat)
        val oppdatertMoteDataMedReferat = aktivitet.moteData.withReferat("Nå er det et referat her")
        val oppdatertAktivitet = endreAktivitet(aktivitet, oppdatertMoteData = oppdatertMoteDataMedReferat)

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
        val aktivitet = nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val møteDataMedEndretReferat = aktivitet.moteData.withReferat("Endret referat")
        val oppdatertAktivitet = endreAktivitet(aktivitet, oppdatertMoteData = møteDataMedEndretReferat)

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
        val møteDataUtenPublisertReferat = nyMoteAktivitet().moteData.withReferatPublisert(false)
        val aktivitet = nyAktivitet(AktivitetTypeData.MOTE).withMoteData(møteDataUtenPublisertReferat)
        val møteDataMedReferatPublisert = aktivitet.moteData.withReferatPublisert(true)
        val oppdatertAktivitet = endreAktivitet(aktivitet, oppdatertMoteData = møteDataMedReferatPublisert)

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
        val aktivitet = nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, oppdatertHistoriskDato = Date())

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
        val aktivitet = nyAktivitet(AktivitetTypeData.EGENAKTIVITET).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, fraDato = Date())

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
        val aktivitet = nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet)))

        assertThat(historikk.size).isEqualTo(1)
        assertThat(historikk[aktivitet.id]!!.endringer).hasSize(1)
        assertThat(historikk.values.first().endringer.size).isEqualTo(1)
        val endring = historikk.values.first().endringer.first()
        assertThat(endring.beskrivelseForBruker).isEqualTo("NAV opprettet aktiviteten")
        assertThat(endring.beskrivelseForVeileder).isEqualTo("${aktivitet.endretAv} opprettet aktiviteten")
    }

    @Test
    fun `Skal lage historikk på at aktivitet avtalt med NAV ble opprettet`() {
        val aktivitet = nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build().withAvtalt(true)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet)))

        assertThat(historikk.size).isEqualTo(1)
        assertThat(historikk[aktivitet.id]!!.endringer).hasSize(1)
        assertThat(historikk.values.first().endringer.size).isEqualTo(1)
        val endring = historikk.values.first().endringer.first()
        assertThat(endring.beskrivelseForBruker).isEqualTo("NAV opprettet aktiviteten. Den er automatisk merket som \"Avtalt med NAV\"")
        assertThat(endring.beskrivelseForVeileder).isEqualTo("${aktivitet.endretAv} opprettet aktiviteten. Den er automatisk merket som \"Avtalt med NAV\"")
    }

    @Test
    fun `Skal lage historikk på at aktivitet ble avtalt med NAV`() {
        val aktivitet = nyAktivitet(AktivitetTypeData.MOTE).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, avtaltMedNav = true)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV merket aktiviteten som \"Avtalt med NAV\"",
            "${oppdatertAktivitet.endretAv} merket aktiviteten som \"Avtalt med NAV\""
        )
    }

    @Test
    fun `Skal lage historikk på forhåndsorientering lest`() {
        val ulestForhåndsorientering = Forhaandsorientering.builder()
            .type(Type.SEND_FORHAANDSORIENTERING)
            .opprettetDato(Date())
            .lestDato(null)
            .id("nyeste")
            .build()
        val aktivitet = nyAktivitet(AktivitetTypeData.MOTE).toBuilder().avtalt(true).forhaandsorientering(ulestForhåndsorientering).build()
        val lestForhåndsorientering = ulestForhåndsorientering.toBuilder().lestDato(Date()).build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, oppdatertForhaandsorientering = lestForhåndsorientering, endretAvType = Innsender.BRUKER, transaksjonsType = AktivitetTransaksjonsType.FORHAANDSORIENTERING_LEST)

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
        val aktivitet = nyStillingFraNav()
        val cvKanDelesData = CvKanDelesData.builder()
            .endretAv("Bruker")
            .kanDeles(false)
            .build()
        val besvartStillingFraNavData = aktivitet.stillingFraNavData.withCvKanDelesData(cvKanDelesData)
        val oppdatertAktivitet = endreAktivitet(aktivitet, endretAvType = Innsender.BRUKER, oppdatertStillingFraNavData = besvartStillingFraNavData)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "Du svarte 'Nei' på spørsmålet \"Er du interessert i denne stillingen?\"",
            "Bruker svarte 'Nei' på spørsmålet \"Er du interessert i denne stillingen?\""
        )
    }

    @Test
    fun `Skal lage historikk på at avtalt dato ble endret`() {
        val aktivitetTilDato = ZonedDateTime.of(2022, 8, 30, 10, 0,0, 0, ZoneId.of("Europe/Oslo"))
        val aktivitet = nyAktivitet(AktivitetTypeData.JOBBSOEKING).toBuilder().avtalt(true).tilDato(
            zonedDateTimeToDate(aktivitetTilDato)).build()
        val oppdatertAktivitetTilDato = ZonedDateTime.of(2022, 9, 2, 11, 0,0, 0, ZoneId.of("Europe/Oslo"))
        val oppdatertAktivitet = endreAktivitet(aktivitet, endretAvType = Innsender.NAV, tilDato = zonedDateTimeToDate(oppdatertAktivitetTilDato))

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV endret til dato på aktiviteten fra 30. august 2022 til 2. september 2022",
            "${oppdatertAktivitet.endretAv} endret til dato på aktiviteten fra 30. august 2022 til 2. september 2022"
        )
        println("NAV endret til dato på aktiviteten fra ${aktivitet.tilDato} til ${oppdatertAktivitet.tilDato}")
    }

    @Test
    fun `Skal lage historikk på søknadsstatus endret`() {
        val aktivitet = nyAktivitet(AktivitetTypeData.STILLING_FRA_NAV).toBuilder().avtalt(true).build()
        val oppdatertAktivitet = endreAktivitet(aktivitet,  endretAvType = Innsender.NAV, oppdatertStatus = AktivitetStatus.GJENNOMFORES)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV flyttet aktiviteten fra Planlagt til Gjennomføres",
            "${oppdatertAktivitet.endretAv} flyttet aktiviteten fra Planlagt til Gjennomføres"
        )
    }

    @Test
    fun `Skal lage historikk på ikke fått jobben`() {
        val stillingFraNavDataVenterStatus = StillingFraNavData.builder().soknadsstatus(Soknadsstatus.VENTER).build()
        val aktivitet = nyAktivitet().stillingFraNavData(stillingFraNavDataVenterStatus).build()
        val stillingFraNavDataIkkeFåttJobben = StillingFraNavData.builder().soknadsstatus(Soknadsstatus.IKKE_FATT_JOBBEN).build()
        val oppdatertAktivitet = endreAktivitet(aktivitet,  endretAvType = Innsender.NAV, oppdatertStillingFraNavData = stillingFraNavDataIkkeFåttJobben)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV endret tilstand til Ikke fått jobben og avsluttet aktiviteten fordi kandidaten har Ikke fått jobben",
            "${oppdatertAktivitet.endretAv} endret tilstand til Ikke fått jobben og avsluttet aktiviteten fordi kandidaten har Ikke fått jobben"
        )
    }

    @Test
    fun `Skal lage historikk på at tittel ble endret`() {
        val aktivitet = nyAktivitet(AktivitetTypeData.EGENAKTIVITET).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, tittel = "Ny tittel")

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV endret tittelen på aktiviteten fra ${aktivitet.tittel} til ${oppdatertAktivitet.tittel}",
            "${oppdatertAktivitet.endretAv} endret tittelen på aktiviteten fra ${aktivitet.tittel} til ${oppdatertAktivitet.tittel}"
        )
    }

    @Test
    fun `Skal lage historikk på at beskrivelse ble endret`() {
        val aktivitet = nyAktivitet(AktivitetTypeData.EGENAKTIVITET).toBuilder().build()
        val oppdatertAktivitet = endreAktivitet(aktivitet, beskrivelse = "Ny beskrivelse")

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV endret beskrivelsen på aktiviteten fra ${aktivitet.beskrivelse} til ${oppdatertAktivitet.beskrivelse}",
            "${oppdatertAktivitet.endretAv} endret beskrivelsen på aktiviteten fra ${aktivitet.beskrivelse} til ${oppdatertAktivitet.beskrivelse}"
        )
    }

    @Test
    fun `Skal lage historikk på fått jobben`() {
        val stillingFraNavDataVenterStatus = StillingFraNavData.builder().soknadsstatus(Soknadsstatus.VENTER).build()
        val aktivitet = nyAktivitet().stillingFraNavData(stillingFraNavDataVenterStatus).build()
        val stillingFraNavDataIkkeFåttJobben = StillingFraNavData.builder().soknadsstatus(Soknadsstatus.FATT_JOBBEN).build()
        val oppdatertAktivitet = endreAktivitet(aktivitet,  endretAvType = Innsender.NAV, oppdatertStillingFraNavData = stillingFraNavDataIkkeFåttJobben)

        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV endret tilstand til Fått jobben \uD83C\uDF89 og avsluttet aktiviteten fordi kandidaten har Fått jobben 🎉",
            "${oppdatertAktivitet.endretAv} endret tilstand til Fått jobben \uD83C\uDF89 og avsluttet aktiviteten fordi kandidaten har Fått jobben \uD83C\uDF89"
        )
    }

    @Test
    fun `Skal lage historikk på etikett endret`() {
        val stillingsoekAktivitetData = AktivitetTypeDataTestBuilder.nyttStillingssok().withStillingsoekEtikett(SOKNAD_SENDT)
        val aktivitet = nyttStillingssok().withStillingsSoekAktivitetData(stillingsoekAktivitetData)
        val oppdatertAktivitet = endreAktivitet(
            aktivitet,
            endretAvType = Innsender.NAV,
            oppdatertStillingsoekAktivitetData = stillingsoekAktivitetData.withStillingsoekEtikett(INNKALT_TIL_INTERVJU)
        )
        val historikk = lagHistorikkForAktiviteter(mapOf(aktivitet.id to listOf(aktivitet, oppdatertAktivitet)))

        assert(
            historikk[aktivitet.id]!!,
            oppdatertAktivitet,
            "NAV endret tilstand til Skal på intervju",
            "${oppdatertAktivitet.endretAv} endret tilstand til Skal på intervju"
        )
    }

    @Test
    fun `Skal mappe innsender riktig`() {
        assertThat(endretAvTekstTilBruker(Innsender.BRUKER)).isEqualTo("Du")
        assertThat(endretAvTekstTilVeileder(Innsender.BRUKER, "")).isEqualTo("Bruker")
        assertThat(endretAvTekstTilArkiv(Innsender.BRUKER, "")).isEqualTo("Bruker")

        assertThat(endretAvTekstTilBruker(Innsender.ARBEIDSGIVER)).isEqualTo("Arbeidsgiver")
        assertThat(endretAvTekstTilVeileder(Innsender.ARBEIDSGIVER, "Bedrift")).isEqualTo("Arbeidsgiver")
        assertThat(endretAvTekstTilArkiv(Innsender.ARBEIDSGIVER, "Bedrift")).isEqualTo("Arbeidsgiver")

        assertThat(endretAvTekstTilBruker(Innsender.TILTAKSARRANGOER)).isEqualTo("Tiltaksarrangør")
        assertThat(endretAvTekstTilVeileder(Innsender.TILTAKSARRANGOER, "Bedrift")).isEqualTo("Tiltaksarrangør Bedrift")
        assertThat(endretAvTekstTilArkiv(Innsender.TILTAKSARRANGOER, "Bedrift")).isEqualTo("Tiltaksarrangør Bedrift")

        assertThat(endretAvTekstTilBruker(Innsender.NAV)).isEqualTo("NAV")
        assertThat(endretAvTekstTilVeileder(Innsender.NAV, "Z12345")).isEqualTo("Z12345")
        assertThat(endretAvTekstTilArkiv(Innsender.NAV, "Z12345")).isEqualTo("NAV")

        assertThat(endretAvTekstTilBruker(Innsender.ARENAIDENT)).isEqualTo("NAV")
        assertThat(endretAvTekstTilVeileder(Innsender.ARENAIDENT, null)).isEqualTo("NAV")
        assertThat(endretAvTekstTilArkiv(Innsender.ARENAIDENT, null)).isEqualTo("NAV")

        assertThat(endretAvTekstTilBruker(Innsender.SYSTEM)).isEqualTo("NAV")
        assertThat(endretAvTekstTilVeileder(Innsender.SYSTEM, "")).isEqualTo("NAV")
        assertThat(endretAvTekstTilArkiv(Innsender.SYSTEM, "")).isEqualTo("NAV")
    }

    @Test
    fun `Sammenslåing av tekst skal håndtere en streng`() {
        assertThat(slåSammenEndringstekster(listOf("NAV har endret status på aktiviteten til Gjennomføres"), "NAV")).isEqualTo("NAV har endret status på aktiviteten til Gjennomføres")
    }

    @Test
    fun `Sammenslåing av tekst skal håndtere to strenger`() {
        assertThat(
            slåSammenEndringstekster(
                listOf(
                    "NAV har endret status på aktiviteten til Gjennomføres",
                    "NAV har endret tidspunkt for møtet"
                ), "NAV"
            )
        ).isEqualTo("NAV har endret status på aktiviteten til Gjennomføres og har endret tidspunkt for møtet")
    }

    @Test
    fun `Sammenslåing av tekst skal håndtere flere enn to strenger`() {
        assertThat(
            slåSammenEndringstekster(
                listOf(
                    "NAV har endret status på aktiviteten til Gjennomføres",
                    "NAV har endret tidspunkt for møtet",
                    "NAV har markert aktiviteten som avtalt"
                ), "NAV"
            )
        ).isEqualTo("NAV har endret status på aktiviteten til Gjennomføres, har endret tidspunkt for møtet og har markert aktiviteten som avtalt")
    }

    private fun assert(
        historikk: Historikk,
        oppdatertAktivitet: AktivitetData,
        beskrivelseForBruker: String,
        beskrivelseForVeileder: String,
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
        endretAvType: Innsender = Innsender.NAV,
        endretDato: Date = Date(),
        endretAv: String = "Z12345",
        avtaltMedNav: Boolean = aktivitet.isAvtalt,
        fraDato: Date? = aktivitet.fraDato,
        tilDato: Date? = aktivitet.tilDato,
        tittel: String = aktivitet.tittel,
        beskrivelse: String = aktivitet.beskrivelse,
        oppdatertStillingsoekAktivitetData: StillingsoekAktivitetData? = aktivitet.stillingsSoekAktivitetData,
        oppdatertMoteData: MoteData? = aktivitet.moteData,
        oppdatertStatus: AktivitetStatus = aktivitet.status,
        oppdatertEgenAktivitetData: EgenAktivitetData? = aktivitet.egenAktivitetData,
        oppdatertHistoriskDato: Date? = aktivitet.historiskDato,
        oppdatertForhaandsorientering: Forhaandsorientering? = aktivitet.forhaandsorientering,
        oppdatertStillingFraNavData: StillingFraNavData? = aktivitet.stillingFraNavData,
        transaksjonsType: AktivitetTransaksjonsType = AktivitetTransaksjonsType.DETALJER_ENDRET
    ): AktivitetData {
        return AktivitetData.builder()
            .id(aktivitet.id) // Hvis denne persisteres, vil den få en ny id fra sekvens
            .aktivitetType(aktivitet.aktivitetType)
            .aktorId(aktivitet.aktorId)
            .versjon(aktivitet.versjon + 1) // Hvis denne persisteres vil den få en ny versjon fra sekvens
            .fraDato(fraDato)
            .tilDato(tilDato)
            .tittel(tittel)
            .beskrivelse(beskrivelse)
            .versjon(aktivitet.versjon + 1)
            .status(oppdatertStatus)
            .avsluttetKommentar(aktivitet.avsluttetKommentar)
            .endretAvType(endretAvType)
            .opprettetDato(aktivitet.opprettetDato)
            .lenke(aktivitet.lenke)
            .transaksjonsType(transaksjonsType)
            .lestAvBrukerForsteGang(aktivitet.lestAvBrukerForsteGang)
            .historiskDato(oppdatertHistoriskDato)
            .endretDato(endretDato)
            .endretAv(endretAv)
            .avtalt(avtaltMedNav)
            .stillingFraNavData(oppdatertStillingFraNavData)
            .stillingsSoekAktivitetData(oppdatertStillingsoekAktivitetData)
            .moteData(oppdatertMoteData)
            .egenAktivitetData(oppdatertEgenAktivitetData)
            .forhaandsorientering(oppdatertForhaandsorientering)
            .malid("2").build()
    }
}

