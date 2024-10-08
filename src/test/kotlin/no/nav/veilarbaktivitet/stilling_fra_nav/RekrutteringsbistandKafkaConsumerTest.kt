package no.nav.veilarbaktivitet.stilling_fra_nav

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import no.nav.common.json.JsonUtils
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate

import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.util.DateUtils
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.ThrowingConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.lang.Boolean
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.BinaryOperator
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.Double
import kotlin.Exception
import kotlin.String
import kotlin.Throws

internal class RekrutteringsbistandKafkaConsumerTest : SpringBootTestBase() {
    @Autowired
    var navCommonKafkaJsonTemplate: KafkaJsonTemplate<RekrutteringsbistandStatusoppdatering?>? = null

    @Autowired
    var kafkaStringTemplate: KafkaStringTemplate? = null

    @Autowired
    var sendBrukernotifikasjonCron: SendBrukernotifikasjonCron? = null

    @Value("\${topic.inn.rekrutteringsbistandStatusoppdatering}")
    private val innRekrutteringsbistandStatusoppdatering: String? = null

    @Autowired
    var meterRegistry: MeterRegistry? = null

    @Autowired
    var brukernotifikasjonAssertsConfig: BrukernotifikasjonAssertsConfig? = null
    var brukernotifikasjonAsserts: BrukernotifikasjonAsserts? = null
    @BeforeEach
    fun setUp() {
        brukernotifikasjonAsserts = BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig)
        meterRegistry!!.find(StillingFraNavMetrikker.REKRUTTERINGSBISTANDSTATUSOPPDATERING).meters()
            .forEach(Consumer { it: Meter? ->
                meterRegistry!!.remove(
                    it
                )
            })
        aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker)
        bestillingsId = aktivitetDTO?.getStillingFraNavData()?.bestillingsId ?: fail("aktivitetDto var null")
    }

    @AfterEach //må kjøres etter hver test for å ikke være static
    fun teardown() {
        StillingFraNavMetrikker(meterRegistry) //for og genskape metrikkene som blir slettet
    }

    @Test
    @Throws(Exception::class)
    fun behandle_CvDelt_Happy_Case_skal_oppdatere_soknadsstatus_og_lage_metrikk() {
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date)
        val aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            INGEN_DETALJER,
            RekrutteringsbistandStatusoppdateringEventType.CV_DELT,
            navIdent,
            tidspunkt
        )
        val aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        SoftAssertions.assertSoftly { assertions: SoftAssertions ->
            assertions.assertThat(aktivitetData_etter.versjon.toInt()).isGreaterThan(aktivitetData_for.versjon.toInt())
            assertions.assertThat(aktivitetData_etter.endretAv).isEqualTo(navIdent)
            assertions.assertThat(aktivitetData_etter.endretDato).isEqualTo(DateUtils.zonedDateTimeToDate(tidspunkt))
            assertions.assertThat(aktivitetData_etter.endretAvType).isEqualTo(Innsender.NAV.name)
            assertions.assertThat(aktivitetData_etter.status).isSameAs(aktivitetData_for.status)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData).isNotNull()
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getSoknadsstatus())
                .isSameAs(Soknadsstatus.CV_DELT)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getLivslopsStatus())
                .isSameAs(aktivitetData_for.stillingFraNavData.getLivslopsStatus())
            assertions.assertAll()
        }
        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(
            java.util.Map.of(
                SUKSESS, 1.0
            )
        )
        val etterCvDelt = brukernotifikasjonAsserts!!.assertBeskjedSendt(mockBruker.fnrAsFnr)
        Assertions.assertThat(etterCvDelt.value().tekst)
            .isEqualTo(RekrutteringsbistandStatusoppdateringService.CV_DELT_DITT_NAV_TEKST)
    }

    @Test
    @Throws(Exception::class)
    fun behandle_ikke_fatt_jobben_uten_svar_om_deling_av_cv_skal_ikke_oppdatere_aktivitet_men_oppdatere_metrikk() {
        val aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        val detaljer = """
                Vi har sett nærmere på CV-en din, dessverre passer ikke kompetansen din helt med behovene til arbeidsgiveren.
                Derfor deler vi ikke CV-en din med arbeidsgiveren denne gangen. Lykke til videre med jobbsøkingen.
                
                """.trimIndent()
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            detaljer,
            RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN,
            navIdent,
            tidspunkt
        )
        Assertions.assertThat(aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id))
            .isEqualTo(aktivitetData_for)
        Assertions.assertThat(antallAvHverArsak())
            .containsExactlyInAnyOrderEntriesOf(
                java.util.Map.of(
                    "Ikke svart", 1.0
                )
            )
    }

    @Test
    fun behandle_fatt_jobben_etter_cv_delt() {
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date)
        val aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        // Rekrutterings bistand sender fatt-jobben
        val detaljer = """KANDIDATLISTE_LUKKET""".trimIndent()
        val nesteTidspunkt = tidspunkt.plusMinutes(2)
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            detaljer,
            RekrutteringsbistandStatusoppdateringEventType.FATT_JOBBEN,
            navIdent,
            nesteTidspunkt
        )
        // Sjekk forventet tilstand
        val aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        SoftAssertions.assertSoftly { assertions: SoftAssertions ->
          assertions.assertThat(aktivitetData_etter.versjon.toInt()).`as`("Forventer ny versjon av aktivitet")
                .isGreaterThan(aktivitetData_for.versjon.toInt())
            assertions.assertThat(aktivitetData_etter.endretAv).isEqualTo(navIdent)
            assertions.assertThat(aktivitetData_etter.endretDato).isEqualTo(DateUtils.zonedDateTimeToDate(nesteTidspunkt))
            assertions.assertThat(aktivitetData_etter.endretAvType).isEqualTo(Innsender.NAV.name)
            assertions.assertThat(aktivitetData_etter.status).isSameAs(AktivitetStatus.FULLFORT)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData).isNotNull()
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getSoknadsstatus())
                .isSameAs(Soknadsstatus.FATT_JOBBEN)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getLivslopsStatus())
                .isSameAs(aktivitetData_for.stillingFraNavData.getLivslopsStatus())
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getDetaljer())
                .isEqualTo(detaljer)
            assertions.assertAll()
        }
        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(
            java.util.Map.of(SUKSESS, 1.0)
        )
    }

    @Test
    @Throws(Exception::class)
    fun behandle_ikke_fatt_jobben_svart_men_cv_er_ikke_delt() {
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date)
        val aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        val ikkeFattJobbenDetaljer = """
                    Vi har fått beskjed om at arbeidsgiveren har ansatt en person. Dessverre var det ikke deg denne gangen.
                    Ansettelsesprosessen er ferdig. Lykke til videre med jobbsøkingen.
                
                """.trimIndent()
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            ikkeFattJobbenDetaljer,
            RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN,
            navIdent,
            tidspunkt
        )
        val aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        Assertions.assertThat(antallAvHverArsak())
            .containsExactlyInAnyOrderEntriesOf(
                java.util.Map.of(
                    SUKSESS, 1.0
                )
            )
        SoftAssertions.assertSoftly { assertions: SoftAssertions ->
            assertions.assertThat(aktivitetData_etter.versjon.toInt()).`as`("Forventer ny versjon av aktivitet")
                .isGreaterThan(aktivitetData_for.versjon.toInt())
            assertions.assertThat(aktivitetData_etter.endretAv).isEqualTo(navIdent)
            assertions.assertThat(aktivitetData_etter.endretAvType).isEqualTo(Innsender.NAV.name)
            assertions.assertThat(aktivitetData_etter.status).isSameAs(AktivitetStatus.FULLFORT)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData).isNotNull()
            assertions.assertAll()
            assertions.assertThat(aktivitetData_etter.stillingFraNavData)
                .satisfies(
                    ThrowingConsumer { stillingFraNavData: StillingFraNavData ->
                        assertions.assertThat(stillingFraNavData.getDetaljer())
                            .isEqualTo(ikkeFattJobbenDetaljer)
                        assertions.assertThat(stillingFraNavData.getSoknadsstatus())
                            .isSameAs(Soknadsstatus.IKKE_FATT_JOBBEN)
                        assertions.assertThat(stillingFraNavData.getLivslopsStatus())
                            .isSameAs(aktivitetData_for.stillingFraNavData.getLivslopsStatus())
                    }
                )
            assertions.assertAll()
        }
    }

    @Test
    @Throws(Exception::class)
    fun behandle_ikke_fatt_jobben_nar_aktivitet_er_avbrutt() {
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date)
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            INGEN_DETALJER,
            RekrutteringsbistandStatusoppdateringEventType.CV_DELT,
            navIdent,
            tidspunkt
        )
        val etterCvDelt = brukernotifikasjonAsserts!!.assertBeskjedSendt(mockBruker.fnrAsFnr)
        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(
            java.util.Map.of(
                SUKSESS, 1.0
            )
        )
        val aktivitetData_etterCvDelt = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        aktivitetTestService.oppdaterAktivitetStatus(
            mockBruker,
            veileder,
            aktivitetData_etterCvDelt,
            AktivitetStatus.AVBRUTT
        )
        val aktivitetData_etterAvbrutt = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        val ikkeFattJobbenDetaljer = """
                    Vi har fått beskjed om at arbeidsgiveren har ansatt en person. Dessverre var det ikke deg denne gangen.
                    Ansettelsesprosessen er ferdig. Lykke til videre med jobbsøkingen.
                
                """.trimIndent()
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            ikkeFattJobbenDetaljer,
            RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN,
            navIdent,
            tidspunkt
        )
        val aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        brukernotifikasjonAsserts!!.assertIngenNyeBeskjeder()
        SoftAssertions.assertSoftly { assertions: SoftAssertions ->
            assertions.assertThat(aktivitetData_etter.endretDato)
                .`as`("Tidspunkt for endring settes til det tidspunktet aktiviteten ble oppdatert, ikke til tidspunktet i Kafka-meldingen")
                .isNotEqualTo(tidspunkt)
            assertions.assertThat(aktivitetData_etter.versjon.toInt()).`as`("Forventer ikke ny versjon av aktivitet")
                .isEqualTo(aktivitetData_etterAvbrutt.versjon.toInt())
            assertions.assertThat(aktivitetData_etter.endretAv).isEqualTo(aktivitetData_etterAvbrutt.endretAv)
            assertions.assertThat(aktivitetData_etter.endretAvType)
                .isEqualTo(aktivitetData_etterAvbrutt.endretAvType)
            assertions.assertThat(aktivitetData_etter.status).isSameAs(AktivitetStatus.AVBRUTT)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData).isNotNull()
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getSoknadsstatus())
                .isSameAs(aktivitetData_etterAvbrutt.stillingFraNavData.getSoknadsstatus())
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getLivslopsStatus())
                .isSameAs(aktivitetData_etterAvbrutt.stillingFraNavData.getLivslopsStatus())
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getDetaljer()).isNull()
            assertions.assertAll()
        }
        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(
            java.util.Map.of(
                SUKSESS, 1.0,
                "Aktivitet AVBRUTT", 1.0
            )
        )
        Assertions.assertThat(etterCvDelt.value().tekst)
            .isEqualTo(RekrutteringsbistandStatusoppdateringService.CV_DELT_DITT_NAV_TEKST)
    }

    @Test
    @Throws(Exception::class)
    fun behandle_ikke_fatt_jobben_etter_cv_delt() {
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date)
        val aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            INGEN_DETALJER,
            RekrutteringsbistandStatusoppdateringEventType.CV_DELT,
            navIdent,
            tidspunkt
        )
        val etterCvDelt = brukernotifikasjonAsserts!!.assertBeskjedSendt(mockBruker.fnrAsFnr)
        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(
            java.util.Map.of(
                SUKSESS, 1.0
            )
        )
        val ikkeFattJobbenDetaljer = """
                    Vi har fått beskjed om at arbeidsgiveren har ansatt en person. Dessverre var det ikke deg denne gangen.
                    Ansettelsesprosessen er ferdig. Lykke til videre med jobbsøkingen.
                
                """.trimIndent()
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            ikkeFattJobbenDetaljer,
            RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN,
            navIdent,
            tidspunkt
        )
        val aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        val etterIkkeFattJobben = brukernotifikasjonAsserts!!.assertBeskjedSendt(mockBruker.fnrAsFnr)
        SoftAssertions.assertSoftly { assertions: SoftAssertions ->
            assertions.assertThat(aktivitetData_etter.endretDato)
                .`as`("Tidspunkt for endring settes til det tidspunktet aktiviteten ble oppdatert, ikke til tidspunktet i Kafka-meldingen")
                .isNotEqualTo(tidspunkt)
            assertions.assertThat(aktivitetData_etter.versjon.toInt()).`as`("Forventer ny versjon av aktivitet")
                .isGreaterThan(aktivitetData_for.versjon.toInt())
            assertions.assertThat(aktivitetData_etter.endretAv).isEqualTo(navIdent)
            assertions.assertThat(aktivitetData_etter.endretAvType).isEqualTo(Innsender.NAV.name)
            assertions.assertThat(aktivitetData_etter.status).isSameAs(AktivitetStatus.FULLFORT)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData).isNotNull()
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getSoknadsstatus())
                .isSameAs(Soknadsstatus.IKKE_FATT_JOBBEN)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getLivslopsStatus())
                .isSameAs(aktivitetData_for.stillingFraNavData.getLivslopsStatus())
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getDetaljer())
                .isEqualTo(ikkeFattJobbenDetaljer)
            assertions.assertAll()
        }
        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(
            java.util.Map.of(
                SUKSESS, 2.0
            )
        )
        Assertions.assertThat(etterCvDelt.value().tekst)
            .isEqualTo(RekrutteringsbistandStatusoppdateringService.CV_DELT_DITT_NAV_TEKST)
        Assertions.assertThat(etterIkkeFattJobben.value().tekst)
            .isEqualTo(RekrutteringsbistandStatusoppdateringService.IKKE_FATT_JOBBEN_TEKST)
    }

    @Test
    @Throws(Exception::class)
    fun behandle_CvDelt_svart_nei_skal_oppdatere_soknadsstatus_og_lage_metrikk() {
        aktivitetTestService.svarPaaDelingAvCv(Boolean.FALSE, mockBruker, veileder, aktivitetDTO, date)
        val aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            INGEN_DETALJER,
            RekrutteringsbistandStatusoppdateringEventType.CV_DELT,
            navIdent,
            tidspunkt
        )
        val aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        Assertions.assertThat(aktivitetData_etter).isEqualTo(aktivitetData_for)
        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(
            java.util.Map.of(
                "Svart NEI", 1.0
            )
        )
        brukernotifikasjonAsserts!!.assertIngenNyeBeskjeder()
    }

    @Test
    @Throws(Exception::class)
    fun duplikat_CvDelt_Skal_ikke_sende_duplikat_brukernotifikasjon() {
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date)
        val aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            INGEN_DETALJER,
            RekrutteringsbistandStatusoppdateringEventType.CV_DELT,
            navIdent,
            tidspunkt
        )
        val aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        SoftAssertions.assertSoftly { assertions: SoftAssertions ->
            assertions.assertThat(aktivitetData_etter.versjon.toInt()).isGreaterThan(aktivitetData_for.versjon.toInt())
            assertions.assertThat(aktivitetData_etter.endretAv).isEqualTo(navIdent)
            assertions.assertThat(aktivitetData_etter.endretAvType).isEqualTo(Innsender.NAV.name)
            assertions.assertThat(aktivitetData_etter.status)
                .isSameAs(aktivitetData_for.status)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData).isNotNull()
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getSoknadsstatus())
                .isSameAs(Soknadsstatus.CV_DELT)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getLivslopsStatus())
                .isSameAs(aktivitetData_for.stillingFraNavData.getLivslopsStatus())
            assertions.assertAll()
        }
        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(
            java.util.Map.of(
                SUKSESS, 1.0
            )
        )
        brukernotifikasjonAsserts!!.assertBeskjedSendt(mockBruker.fnrAsFnr)
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            INGEN_DETALJER,
            RekrutteringsbistandStatusoppdateringEventType.CV_DELT,
            navIdent,
            tidspunkt
        )
        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(
            java.util.Map.of(
                SUKSESS, 1.0,
                "Allerede delt", 1.0
            )
        )
        Assertions.assertThat(aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id))
            .isEqualTo(aktivitetData_etter)
        brukernotifikasjonAsserts!!.assertIngenNyeBeskjeder()
    }

    @Test
    @Throws(
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    fun happy_case_forste_gode_melding_vi_fikk_skal_oppdatere_soknadsstatus_og_lage_metrikk() {
        val aktivitetDTO = aktivitetTestService.opprettStillingFraNav(mockBruker)
        val date = Date.from(Instant.ofEpochSecond(1))
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date)
        val aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.id)
        val bestillingsId = aktivitetDTO.stillingFraNavData.bestillingsId
        val sendtStatusoppdatering = JsonUtils.fromJson(
            """
                                {
                                "type":"CV_DELT",
                                "detaljer":"",
                                "utførtAvNavIdent":"Z314159",
                                "tidspunkt":"2022-08-09T14:24:49.124+02:00"
                                }
                                
                                """
                .trimIndent(), RekrutteringsbistandStatusoppdatering::class.java
        )
        val sendResult = navCommonKafkaJsonTemplate!!.send(
            innRekrutteringsbistandStatusoppdatering,
            bestillingsId,
            sendtStatusoppdatering
        )[5, TimeUnit.SECONDS]
        kafkaTestService.assertErKonsumert(innRekrutteringsbistandStatusoppdatering, sendResult.recordMetadata.offset())
        val aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO.id)
        SoftAssertions.assertSoftly { assertions: SoftAssertions ->
            assertions.assertThat(aktivitetData_etter.versjon.toInt()).isGreaterThan(aktivitetData_for.versjon.toInt())
            assertions.assertThat(aktivitetData_etter.endretAv).isEqualTo("Z314159")
            assertions.assertThat(aktivitetData_etter.endretAvType).isEqualTo(Innsender.NAV.name)
            assertions.assertThat(aktivitetData_etter.status)
                .isSameAs(aktivitetData_for.status)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData).isNotNull()
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getSoknadsstatus())
                .isSameAs(Soknadsstatus.CV_DELT)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getLivslopsStatus())
                .isSameAs(aktivitetData_for.stillingFraNavData.getLivslopsStatus())
            assertions.assertAll()
        }
        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(
            java.util.Map.of(
                SUKSESS, 1.0
            )
        )
        val etterCvDelt = brukernotifikasjonAsserts!!.assertBeskjedSendt(mockBruker.fnrAsFnr)
        Assertions.assertThat(etterCvDelt.value().tekst)
            .isEqualTo(RekrutteringsbistandStatusoppdateringService.CV_DELT_DITT_NAV_TEKST)
    }

    @Test
    @Throws(
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    fun hvis_feil_i_json_skal_vi_ikke_endre_aktivitet_og_lage_metrikk() {
        aktivitetTestService.svarPaaDelingAvCv(true, mockBruker, veileder, aktivitetDTO, date)
        val aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        val sendtStatusoppdatering = """
                {
                    "type":"CV_DELT",
                    "detaljer":"",
                    "tidspunkt":2022-08-03T16:31:32.848+02:00
                }
                
                """.trimIndent()
        val sendResult =
            kafkaStringTemplate!!.send(
                innRekrutteringsbistandStatusoppdatering,
                bestillingsId,
                sendtStatusoppdatering
            )[5, TimeUnit.SECONDS]
        kafkaTestService.assertErKonsumert(innRekrutteringsbistandStatusoppdatering, sendResult.recordMetadata.offset())
        val aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        Assertions.assertThat(antallAvHverArsak()).containsExactlyInAnyOrderEntriesOf(
            java.util.Map.of(
                "Ugyldig melding", 1.0
            )
        )
        Assertions.assertThat(aktivitetData_etter).isEqualTo(aktivitetData_for)
        brukernotifikasjonAsserts!!.assertIngenNyeBeskjeder()
    }

    @Test
    @Throws(
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    fun skal_ignorere_cvdelt_oppdatering_hvis_ikke_svart() {
        val aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            DETALJER_TEKST,
            RekrutteringsbistandStatusoppdateringEventType.CV_DELT,
            navIdent,
            tidspunkt
        )
        Assertions.assertThat(aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id))
            .isEqualTo(aktivitetData_for)
        Assertions.assertThat(antallAvHverArsak())
            .containsExactlyInAnyOrderEntriesOf(
                java.util.Map.of(
                    "Ikke svart", 1.0
                )
            )
        brukernotifikasjonAsserts!!.assertIngenNyeBeskjeder()
    }

    @Test
    @Throws(
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    fun skal_ignorere_cvdelt_oppdatering_hvis_aktivitet_ikke_finnes() {
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            "666",
            DETALJER_TEKST,
            RekrutteringsbistandStatusoppdateringEventType.CV_DELT,
            navIdent,
            tidspunkt
        )
        Assertions.assertThat(antallAvHverArsak())
            .containsExactlyInAnyOrderEntriesOf(
                java.util.Map.of(
                    "Bestillingsid ikke funnet", 1.0
                )
            )
        brukernotifikasjonAsserts!!.assertIngenNyeBeskjeder()
    }

    @Test
    @Throws(
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    fun skal_ignorere_cvdelt_oppdatering_hvis_NEI_pa_deling_av_cv() {
        aktivitetTestService.svarPaaDelingAvCv(
            NEI,
            mockBruker,
            veileder,
            aktivitetDTO,
            Date.from(Instant.ofEpochSecond(1))
        )
        val aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            DETALJER_TEKST,
            RekrutteringsbistandStatusoppdateringEventType.CV_DELT,
            navIdent,
            tidspunkt
        )
        Assertions.assertThat(aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id))
            .isEqualTo(aktivitetData_for)
        Assertions.assertThat(antallAvHverArsak())
            .containsExactlyInAnyOrderEntriesOf(
                java.util.Map.of(
                    "Svart NEI", 1.0
                )
            )
        brukernotifikasjonAsserts!!.assertIngenNyeBeskjeder()
    }

    @Test
    @Throws(
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    fun skal_ignorere_cvdelt_oppdatering_hvis_aktivitet_er_i_status_FULLFORT() {
        val aktivitetDTO_svartJA = aktivitetTestService.svarPaaDelingAvCv(
            JA, mockBruker, veileder, aktivitetDTO, Date.from(
                Instant.ofEpochSecond(1)
            )
        )
        aktivitetTestService.oppdaterAktivitetStatus(
            mockBruker,
            veileder,
            aktivitetDTO_svartJA,
            AktivitetStatus.FULLFORT
        )
        val aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            DETALJER_TEKST,
            RekrutteringsbistandStatusoppdateringEventType.CV_DELT,
            navIdent,
            tidspunkt
        )
        val aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        SoftAssertions.assertSoftly { assertions: SoftAssertions ->
            assertions.assertThat(aktivitetData_etter.endretDato)
                .`as`("Tidspunkt for endring settes til det tidspunktet aktiviteten ble oppdatert, ikke til tidspunktet i Kafka-meldingen")
                .isNotEqualTo(tidspunkt)
            assertions.assertThat(aktivitetData_etter.versjon.toInt()).isGreaterThan(aktivitetData_for.versjon.toInt())
            assertions.assertThat(aktivitetData_etter.endretAv).isEqualTo(navIdent)
            assertions.assertThat(aktivitetData_etter.endretAvType).isEqualTo(Innsender.NAV.name)
            assertions.assertThat(aktivitetData_etter.status)
                .isSameAs(aktivitetData_for.status)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData).isNotNull()
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getSoknadsstatus())
                .isSameAs(Soknadsstatus.CV_DELT)
            assertions.assertThat(aktivitetData_etter.stillingFraNavData.getLivslopsStatus())
                .isSameAs(aktivitetData_for.stillingFraNavData.getLivslopsStatus())
            assertions.assertAll()
        }
        Assertions.assertThat(antallAvHverArsak())
            .containsExactlyInAnyOrderEntriesOf(
                java.util.Map.of(
                    SUKSESS, 1.0
                )
            )
        val etterCvDelt = brukernotifikasjonAsserts!!.assertBeskjedSendt(mockBruker.fnrAsFnr)
        Assertions.assertThat(etterCvDelt.value().tekst)
            .isEqualTo(RekrutteringsbistandStatusoppdateringService.CV_DELT_DITT_NAV_TEKST)
    }

    @Test
    @Throws(
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    fun skal_ignorere_cvdelt_aktivitet_er_i_status_AVBRUTT() {
        val aktivitetDTO_svartJA = aktivitetTestService.svarPaaDelingAvCv(
            JA, mockBruker, veileder, aktivitetDTO, Date.from(
                Instant.ofEpochSecond(1)
            )
        )
        aktivitetTestService.oppdaterAktivitetStatus(
            mockBruker,
            veileder,
            aktivitetDTO_svartJA,
            AktivitetStatus.AVBRUTT
        )
        val aktivitetData_for = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        aktivitetTestService.mottaOppdateringFraRekrutteringsbistand(
            bestillingsId,
            DETALJER_TEKST,
            RekrutteringsbistandStatusoppdateringEventType.CV_DELT,
            navIdent,
            tidspunkt
        )
        val aktivitetData_etter = aktivitetTestService.hentAktivitet(mockBruker, veileder, aktivitetDTO!!.id)
        Assertions.assertThat(antallAvHverArsak())
            .containsExactlyInAnyOrderEntriesOf(
                java.util.Map.of(
                    "Aktivitet AVBRUTT", 1.0
                )
            )
        Assertions.assertThat(aktivitetData_etter).isEqualTo(aktivitetData_for)
        brukernotifikasjonAsserts!!.assertIngenNyeBeskjeder()
    }

    private fun antallAvHverArsak(): Map<String, Double> {
        return meterRegistry!!.find(StillingFraNavMetrikker.REKRUTTERINGSBISTANDSTATUSOPPDATERING).counters().stream()
            .collect(
                Collectors.toMap(
                    Function { c: Counter ->
                        c.id.getTag("reason")
                    },
                    Function { obj: Counter -> obj.count() },
                    BinaryOperator { a: Double, b: Double -> java.lang.Double.sum(a, b) })
            )
    }

    private val DETALJER_TEKST = ""
    private val JA = Boolean.TRUE
    private val NEI = Boolean.FALSE
    private val tidspunkt = ZonedDateTime.of(2020, 4, 5, 16, 17, 0, 0, ZoneId.systemDefault())
    private val navIdent = "E271828"
    private val mockBruker = navMockService.createHappyBruker()
    private val veileder =  navMockService.createVeileder(mockBruker)
    private val date = Date.from(Instant.ofEpochSecond(1))
    private val SUKSESS = ""
    private val INGEN_DETALJER = ""
    private var aktivitetDTO: AktivitetDTO? = null
    private var bestillingsId: String? = null
}
