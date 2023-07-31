package no.nav.veilarbaktivitet.aktivitetskort

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import no.nav.common.json.JsonUtils
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.types.identer.NavIdent
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType
import no.nav.veilarbaktivitet.aktivitet.domain.Ident
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.aktivitet.dto.EksternAktivitetDTO
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortProducerUtil.extraFieldRecord
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortProducerUtil.invalidDateFieldRecord
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortProducerUtil.kafkaAktivitetWrapper
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortProducerUtil.missingFieldRecord
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling
import no.nav.veilarbaktivitet.aktivitetskort.dto.KafkaAktivitetskortWrapperDTO
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.*
import no.nav.veilarbaktivitet.aktivitetskort.feil.*
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDto
import no.nav.veilarbaktivitet.aktivitetskort.service.AktivitetskortService
import no.nav.veilarbaktivitet.aktivitetskort.service.TiltakMigreringCronService
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService
import no.nav.veilarbaktivitet.mock_nav_modell.WireMockUtil
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.testutils.AktivitetskortTestBuilder
import no.nav.veilarbaktivitet.util.DateUtils
import no.nav.veilarbaktivitet.util.KafkaTestService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

internal class AktivitetskortConsumerIntegrationTest : SpringBootTestBase() {

    @Autowired
    var producerClient: KafkaProducerClient<String, String>? = null

    @Autowired
    var aktivitetskortConsumer: AktivitetskortConsumer? = null

    @Autowired
    var messageDAO: AktivitetsMessageDAO? = null

    @Autowired
    var aktivitetskortService: AktivitetskortService? = null

    @Autowired
    var tiltakMigreringCronService: TiltakMigreringCronService? = null

    @Autowired
    var meterRegistry: MeterRegistry? = null

    @Value("\${topic.inn.aktivitetskort}")
    var topic: String? = null

    @Value("\${topic.ut.aktivitetskort-feil}")
    var aktivitetskortFeilTopic: String? = null

    @Value("\${topic.ut.aktivitetskort-idmapping}")
    var aktivitetskortIdMappingTopic: String? = null

    @Autowired
    var brukernotifikasjonAssertsConfig: BrukernotifikasjonAssertsConfig? = null
    var brukernotifikasjonAsserts: BrukernotifikasjonAsserts? = null
    var aktivitetskortFeilConsumer: org.apache.kafka.clients.consumer.Consumer<String, String>? = null
    var aktivitetskortIdMappingConsumer: org.apache.kafka.clients.consumer.Consumer<String, String>? = null
    @BeforeEach
    fun cleanupBetweenTests() {
        Mockito.`when`(unleashClient.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)).thenReturn(true)
        aktivitetskortFeilConsumer = kafkaTestService.createStringStringConsumer(aktivitetskortFeilTopic)
        aktivitetskortIdMappingConsumer = kafkaTestService.createStringStringConsumer(aktivitetskortIdMappingTopic)
        brukernotifikasjonAsserts = BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig)
    }

    fun meldingContext(): ArenaMeldingHeaders {
        return ArenaMeldingHeaders(ArenaId("ARENATA123"), "MIDL")
    }

    fun meldingContext(eksternRefanseId: ArenaId?): ArenaMeldingHeaders {
        return ArenaMeldingHeaders(eksternRefanseId, "MIDL")
    }

    fun meldingContext(eksternRefanseId: ArenaId?, arenaTiltakskode: String?): ArenaMeldingHeaders {
        return ArenaMeldingHeaders(eksternRefanseId, arenaTiltakskode)
    }

    private val defaultcontext = meldingContext()
    fun aktivitetskort(funksjonellId: UUID?, aktivitetStatus: AktivitetStatus?, bruker: MockBruker = mockBruker): Aktivitetskort {
        return AktivitetskortTestBuilder.ny(
            funksjonellId,
            aktivitetStatus,
            endretDato,
            bruker
        )
    }

    private fun assertFeilmeldingPublished(
        funksjonellId: UUID,
        errorClass: Class<out Exception?>,
        feilmelding: String = ""
    ) {
        val singleRecord = KafkaTestUtils.getSingleRecord(
            aktivitetskortFeilConsumer,
            aktivitetskortFeilTopic,
            KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION
        )
        val payload = JsonUtils.fromJson(singleRecord.value(), AktivitetskortFeilMelding::class.java)
        assertThat(singleRecord.key()).isEqualTo(funksjonellId.toString())
        assertThat(payload.errorMessage).contains(errorClass.name)
        assertThat(payload.errorMessage).contains(feilmelding)
    }

    private fun assertIdMappingPublished(funksjonellId: UUID, arenaId: ArenaId) {
        val singleRecord = KafkaTestUtils.getSingleRecord(
            aktivitetskortIdMappingConsumer,
            aktivitetskortIdMappingTopic,
            KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION
        )
        val payload = JsonUtils.fromJson(singleRecord.value(), IdMappingDto::class.java)
        assertThat(singleRecord.key()).isEqualTo(funksjonellId.toString())
        assertThat(payload.arenaId).isEqualTo(arenaId)
    }

    @Test
    fun happy_case_upsert_ny_arenatiltaksaktivitet() {
        //trenges for og teste med count hvis ikke må man også matche på tags for å få testet counten
        //burde man endre på metrikkene her? kan man vite en fulstendig liste av aktiviteskort og skilde?
        meterRegistry!!.find(AktivitetskortMetrikker.AKTIVITETSKORT_UPSERT).meters()
            .forEach(java.util.function.Consumer { it: Meter? -> meterRegistry!!.remove(it) })
        val funksjonellId = UUID.randomUUID()
        val actual = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
        val arenata123 = ArenaId("ARENATA123")
        val kontekst = meldingContext(arenata123, "MIDL")
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(listOf(actual), listOf(kontekst))
        val count = meterRegistry!!.find(AktivitetskortMetrikker.AKTIVITETSKORT_UPSERT).counter().count()
        assertThat(count).isEqualTo(1.0)
        val aktivitet = hentAktivitet(funksjonellId)
        assertThat(aktivitet.type).isEqualTo(AktivitetTypeDTO.EKSTERNAKTIVITET)
        assertEquals(AktivitetTransaksjonsType.OPPRETTET, aktivitet.transaksjonsType)
        assertEquals(AktivitetStatus.PLANLAGT, aktivitet.status)
        assertTrue(aktivitet.isAvtalt)
        assertEquals(Innsender.ARENAIDENT.name, aktivitet.endretAvType)
        assertEquals(
            aktivitet.eksternAktivitet, EksternAktivitetDTO(
                AktivitetskortType.ARENA_TILTAK,
                null, emptyList(),
                actual.detaljer,
                actual.etiketter
            )
        )
        assertIdMappingPublished(funksjonellId, arenata123)
    }

    @Test
    fun aktiviteter_opprettet_av_bruker_skal_ha_riktig_endretAv_verdi() {
        val brukerIdent = "12129312122"
        val aktivitetskort = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
            .copy(endretAv = Ident(
                brukerIdent,
                IdentType.PERSONBRUKERIDENT
            ))
        val kafkaAktivitetskortWrapperDTO = AktivitetskortTestBuilder.aktivitetskortMelding(
            aktivitetskort, UUID.randomUUID(), "TEAM_TILTAK", AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(kafkaAktivitetskortWrapperDTO))
        val resultat = hentAktivitet(aktivitetskort.id)
        assertThat(resultat.endretAv).isEqualTo(brukerIdent)
        assertThat(resultat.endretAvType).isEqualTo(Innsender.BRUKER.toString())
    }

    @Test
    fun ekstern_aktivitet_skal_ha_oppfolgingsperiode() {
        val funksjonellId = UUID.randomUUID()
        val actual = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
        val wrapperDTO = KafkaAktivitetskortWrapperDTO(
            aktivitetskortType = AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
//            actionType = ActionType.UPSERT_AKTIVITETSKORT_V1,
            aktivitetskort = actual,
            source = "source",
            messageId = UUID.randomUUID())
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(wrapperDTO))
        val aktivitet = hentAktivitet(funksjonellId)
        assertEquals(mockBruker.oppfolgingsperiode, aktivitet.oppfolgingsperiodeId)
    }

    @Test
    fun historisk_arenatiltak_aktivitet_skal_ha_oppfolgingsperiode() {
        val funksjonellId = UUID.randomUUID()
        val actual = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
            .copy(endretTidspunkt = ZonedDateTime.now().minusDays(75))
        val arenata123 = ArenaId("ARENATA123")
        val kontekst = meldingContext(arenata123, "MIDLONNTIL")
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(listOf(actual), listOf(kontekst))
        val aktivitetFoer = hentAktivitet(funksjonellId)
        assertThat(aktivitetFoer.oppfolgingsperiodeId).isNotNull()
        assertThat(aktivitetFoer.isHistorisk).isFalse()
        val aktivitetFoerOpprettetSomHistorisk = jdbcTemplate.queryForObject(
            """
                SELECT opprettet_som_historisk
                FROM EKSTERNAKTIVITET
                WHERE AKTIVITET_ID = ?
                ORDER BY VERSJON desc
                FETCH NEXT 1 ROW ONLY 
                
                """.trimIndent(), Boolean::class.javaPrimitiveType, aktivitetFoer.id
        )
        assertThat(aktivitetFoerOpprettetSomHistorisk).isTrue()
        tiltakMigreringCronService!!.settTiltakOpprettetSomHistoriskTilHistorisk()
        val aktivitetEtter = hentAktivitet(funksjonellId)
        assertThat(aktivitetEtter.isHistorisk).isTrue()
    }

    @Test
    fun lonnstilskudd_på_bruker_som_ikke_er_under_oppfolging_skal_feile() {
        val funksjonellId = UUID.randomUUID()
        val brukerUtenOppfolging = MockNavService.createBruker(
            BrukerOptions.happyBruker().toBuilder().underOppfolging(false).build()
        )
        val actual = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT, brukerUtenOppfolging)
        val wrapperDTO = KafkaAktivitetskortWrapperDTO(
            aktivitetskortType = AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
            aktivitetskort = actual,
            source = "source",
            messageId = UUID.randomUUID())
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(wrapperDTO))
        assertFeilmeldingPublished(
            funksjonellId,
            ManglerOppfolgingsperiodeFeil::class.java,
            "Finner ingen passende oppfølgingsperiode for aktivitetskortet."
        )
    }

    @Test
    fun happy_case_upsert_status_existing_tiltaksaktivitet() {
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
        val meldingContext = meldingContext(ArenaId("ARENATA123"), "MIDL")
        val annenVeileder = Ident("ANNEN_NAV_IDENT", Innsender.ARENAIDENT)
        val tiltaksaktivitetUpdate: Aktivitetskort = aktivitetskort(funksjonellId, AktivitetStatus.GJENNOMFORES)
            .copy(endretAv = annenVeileder)
        val updatemeldingContext = meldingContext(ArenaId("ARENATA123"), "MIDL")
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(
                tiltaksaktivitet,
                tiltaksaktivitetUpdate
            ), listOf(meldingContext, updatemeldingContext)
        )
        val aktivitet = hentAktivitet(funksjonellId)
        assertThat(aktivitet.type).isEqualTo(AktivitetTypeDTO.EKSTERNAKTIVITET)
        assertNotNull(aktivitet)
        assertThat(tiltaksaktivitet.endretTidspunkt)
            .isCloseTo(DateUtils.dateToZonedDateTime(aktivitet.endretDato), within(1, ChronoUnit.MILLIS))
        assertThat(aktivitet.endretAv).isEqualTo(annenVeileder.ident)
        assertEquals(AktivitetStatus.GJENNOMFORES, aktivitet.status)
        assertEquals(
            AktivitetTransaksjonsType.STATUS_ENDRET,
            aktivitet.transaksjonsType
        )
    }

    @Test
    fun oppdater_tiltaksaktivitet_fra_avtalt_til_ikke_avtalt_skal_throwe() {
        val funksjonellId = UUID.randomUUID()
        val lonnstilskuddAktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
            .copy(avtaltMedNav = true)
        val melding1 = AktivitetskortTestBuilder.aktivitetskortMelding(
            lonnstilskuddAktivitet, UUID.randomUUID(), "TEAM_TILTAK", AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD
        )
        val lonnstilskuddAktivitetUpdate: Aktivitetskort = aktivitetskort(funksjonellId, AktivitetStatus.GJENNOMFORES)
            .copy(avtaltMedNav = false)
        val melding2 = AktivitetskortTestBuilder.aktivitetskortMelding(
            lonnstilskuddAktivitetUpdate, UUID.randomUUID(), "TEAM_TILTAK", AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(melding1, melding2))
        val aktivitet = hentAktivitet(funksjonellId)
        assertNotNull(aktivitet)
        assertFeilmeldingPublished(
            funksjonellId,
            UlovligEndringFeil::class.java,
            "Kan ikke oppdatere fra avtalt til ikke-avtalt"
        )
    }

    @Test
    fun oppdater_tiltaksaktivitet_endre_bruker_skal_throwe() {
        val funksjonellId = UUID.randomUUID()
        val lonnstilskuddAktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
        val melding1 = AktivitetskortTestBuilder.aktivitetskortMelding(
            lonnstilskuddAktivitet, UUID.randomUUID(), "TEAM_TILTAK", AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD
        )
        val mockBruker2 = MockNavService.createHappyBruker()
        val lonnstilskuddAktivitetUpdate: Aktivitetskort = aktivitetskort(funksjonellId, AktivitetStatus.GJENNOMFORES)
            .copy(personIdent = mockBruker2.fnr)
        val melding2 = AktivitetskortTestBuilder.aktivitetskortMelding(
            lonnstilskuddAktivitetUpdate, UUID.randomUUID(), "TEAM_TILTAK", AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(melding1, melding2))
        val aktivitet = hentAktivitet(funksjonellId)
        assertNotNull(aktivitet)
        assertFeilmeldingPublished(
            funksjonellId,
            UlovligEndringFeil::class.java,
            "Kan ikke endre bruker på samme aktivitetskort"
        )
    }

    @Test
    fun duplikat_melding_bare_1_opprettet_transaksjon() {
        val funksjonellId = UUID.randomUUID()
        val aktivitetskort = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
        val kafkaAktivitetskortWrapperDTO = AktivitetskortTestBuilder.aktivitetskortMelding(aktivitetskort)
        val context = meldingContext()
        val producerRecord =
            aktivitetTestService.makeAktivitetskortProducerRecord(kafkaAktivitetskortWrapperDTO, context) as ProducerRecord<String, String>
        producerClient!!.sendSync(producerRecord)
        val recordMetadataDuplicate = producerClient!!.sendSync(producerRecord)
        kafkaTestService.assertErKonsumert(
            topic,
            NavCommonKafkaConfig.CONSUMER_GROUP_ID,
            recordMetadataDuplicate.offset()
        )
        val aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter.stream()
            .filter { a: AktivitetDTO -> a.funksjonellId == funksjonellId }
            .toList()
        assertEquals(1, aktiviteter.size)
        assertEquals(
            AktivitetTransaksjonsType.OPPRETTET,
            aktiviteter.stream().findFirst().get().transaksjonsType
        )
    }

    @Test
    fun oppdatering_av_detaljer_gir_riktig_transaksjon() {
        val funksjonellId = UUID.randomUUID()
        val context = meldingContext()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
        val tiltaksaktivitetEndret: Aktivitetskort = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
            .copy(detaljer = listOf(
                Attributt(
                    "Tiltaksnavn",
                    "Nytt navn"
                )
            ))
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(
                tiltaksaktivitet,
                tiltaksaktivitetEndret
            ), listOf(context, context)
        )
        val aktivitet = hentAktivitet(funksjonellId)
        assertEquals(
            AktivitetTransaksjonsType.DETALJER_ENDRET,
            aktivitet.transaksjonsType
        )
    }

    @Test
    fun oppdatering_status_og_detaljer_gir_4_transaksjoner() {
        val funksjonellId = UUID.randomUUID()
        val context = meldingContext()
        val tiltaksaktivitet: Aktivitetskort = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
            .copy(avtaltMedNav = false)
        val etikett = Etikett("FÅTT_PLASS")
        val tiltaksaktivitetEndret: Aktivitetskort =
            aktivitetskort(funksjonellId, AktivitetStatus.GJENNOMFORES)
                .copy(avtaltMedNav = true, etiketter = listOf(etikett))
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(
                tiltaksaktivitet,
                tiltaksaktivitetEndret
            ), listOf(context, context)
        )
        val aktivitetId = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter.stream()
            .findFirst().get().id
        val aktivitetVersjoner = aktivitetTestService.hentVersjoner(aktivitetId, mockBruker, mockBruker)
        assertEquals(4, aktivitetVersjoner.size)
        val sisteVersjon = aktivitetVersjoner[0]
        assertThat(sisteVersjon.isAvtalt).isTrue()
        assertThat(sisteVersjon.eksternAktivitet.etiketter).isEqualTo(tiltaksaktivitetEndret.etiketter)
        assertThat(sisteVersjon.status).isEqualTo(AktivitetStatus.GJENNOMFORES)
        assertEquals(
            AktivitetTransaksjonsType.STATUS_ENDRET,
            aktivitetVersjoner[0].transaksjonsType
        )
        assertEquals(
            AktivitetTransaksjonsType.DETALJER_ENDRET,
            aktivitetVersjoner[1].transaksjonsType
        )
        assertEquals(
            AktivitetTransaksjonsType.AVTALT,
            aktivitetVersjoner[2].transaksjonsType
        )
        assertEquals(
            AktivitetTransaksjonsType.OPPRETTET,
            aktivitetVersjoner[3].transaksjonsType
        )
    }

    @Test
    fun endretTidspunkt_skal_settes_fra_melding() {
        val funksjonellId = UUID.randomUUID()
        val context = meldingContext()
        val aktivitetskort: Aktivitetskort = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
            .copy(endretTidspunkt = endretDato)
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(listOf(aktivitetskort), listOf(context))
        val aktivitet = hentAktivitet(funksjonellId)
        val endretDatoInstant = endretDato.toInstant()
        assertThat(aktivitet.endretDato).isEqualTo(endretDatoInstant)
    }

    @Test
    fun skal_skippe_gamle_meldinger_etter_ny_melding() {
        val funksjonellId = UUID.randomUUID()
        val nyesteNavn = "Nytt navn"
        val context = meldingContext()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
            .copy(detaljer = listOf(
                Attributt(
                    "Tiltaksnavn",
                    "Gammelt navn"
                )
            ))
        val tiltaksMelding = AktivitetskortTestBuilder.aktivitetskortMelding(tiltaksaktivitet)
        val tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
            .copy(detaljer = listOf(
                Attributt(
                    "Tiltaksnavn",
                    nyesteNavn
                )
            ))
        val tiltaksMeldingEndret = AktivitetskortTestBuilder.aktivitetskortMelding(tiltaksaktivitetEndret)
        aktivitetTestService.opprettEksterntAktivitetsKort(
            listOf(
                tiltaksMelding,
                tiltaksMeldingEndret,
                tiltaksMelding
            ), listOf(context, context, context)
        )
        val aktivitet = hentAktivitet(funksjonellId)
        val detaljer = aktivitet.eksternAktivitet.detaljer
        assertThat(detaljer.stream().filter { it: Attributt -> it.label == "Tiltaksnavn" }).hasSize(1)
        assertThat(detaljer).containsOnlyOnceElementsOf(listOf(
            Attributt(
                "Tiltaksnavn",
                nyesteNavn
            )
        ))
    }

    private fun hentAktivitet(funksjonellId: UUID): AktivitetDTO {
        return aktivitetTestService.hentAktiviteterForFnr(mockBruker, veileder)
            .aktiviteter
            .first { a: AktivitetDTO -> a.funksjonellId == funksjonellId }
    }

    @Test
    fun avbrutt_aktivitet_kan_endres() {
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.AVBRUTT)
        val tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
        val context = meldingContext()
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(
                tiltaksaktivitet,
                tiltaksaktivitetEndret
            ), listOf(context, context)
        )
        val aktivitet = hentAktivitet(funksjonellId)
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
    }

    @Test
    fun fullfort_aktivitet_kan_endres() {
        val tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.FULLFORT)
        val tiltaksaktivitetEndret = aktivitetskort(tiltaksaktivitet.id, AktivitetStatus.PLANLAGT)
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(
                tiltaksaktivitet,
                tiltaksaktivitetEndret
            ), listOf(defaultcontext, defaultcontext)
        )
        val aktivitet = hentAktivitet(tiltaksaktivitet.id)
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
    }

    @Test
    fun aktivitet_kan_settes_til_avbrutt() {
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
        val tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.AVBRUTT)
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(
                tiltaksaktivitet,
                tiltaksaktivitetEndret
            ), listOf(defaultcontext, defaultcontext)
        )
        val aktivitet = hentAktivitet(funksjonellId)
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.AVBRUTT)
    }

    @Test
    fun invalid_messages_should_catch_deserializer_error() {
        val messages = listOf(
            missingFieldRecord(mockBruker.fnrAsFnr),
            extraFieldRecord(mockBruker.fnrAsFnr),
            invalidDateFieldRecord(mockBruker.fnrAsFnr)
        )
        val headers: Iterable<Header> = listOf<Header>(
            RecordHeader(
                AktivitetsbestillingCreator.HEADER_EKSTERN_REFERANSE_ID,
                ArenaId("ARENATA123").id().toByteArray()
            ),
            RecordHeader(AktivitetsbestillingCreator.HEADER_EKSTERN_ARENA_TILTAKSKODE, "MIDLONS".toByteArray())
        )
        val lastRecordMetadata = messages.stream()
            .map { (json, messageId) ->
                ProducerRecord(
                    topic,
                    null,
                    messageId.toString(),
                    json,
                    headers
                )
            }
            .map { record: ProducerRecord<String, String>? -> producerClient!!.sendSync(record) }
            .reduce { first: RecordMetadata?, second: RecordMetadata -> second }
            .get()
        kafkaTestService.assertErKonsumert(topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, lastRecordMetadata.offset())
        val records = KafkaTestUtils.getRecords(
            aktivitetskortFeilConsumer,
            KafkaTestService.DEFAULT_WAIT_TIMEOUT_DURATION,
            messages.size
        )
        assertThat(records.count()).isEqualTo(messages.size)
    }

    @Test
    fun should_catch_ugyldigident_error() {
        WireMockUtil.aktorUtenGjeldende(mockBruker.fnr, mockBruker.aktorId)
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(tiltaksaktivitet),
            listOf(defaultcontext)
        )
        assertFeilmeldingPublished(
            funksjonellId,
            UgyldigIdentFeil::class.java
        )
    }

    @Test
    fun should_throw_runtime_exception_on_missing_arena_headers() {
        var aktivitetWrapper = kafkaAktivitetWrapper(mockBruker.fnrAsFnr)
        aktivitetWrapper = aktivitetWrapper.copy(aktivitetskortType = AktivitetskortType.ARENA_TILTAK)
        val h1 = RecordHeader("NONSENSE_HEADER", "DUMMYVALUE".toByteArray())
        val record = ConsumerRecord(
            topic,
            0,
            0,
            aktivitetWrapper.aktivitetskort.id.toString(),
            JsonUtils.toJson(aktivitetWrapper)
        )
        record.headers().add(h1)
        /* Call consume directly to avoid retry / waiting for message to be consumed */
        val runtimeException = assertThrows(
            RuntimeException::class.java
        ) { aktivitetskortConsumer!!.consume(record) }
        assertThat(runtimeException.message).isEqualTo("Mangler Arena Header for ArenaTiltak aktivitetskort")
    }

    @Test
    fun should_throw_runtime_exception_on_missing_messageId() {
        val aktivitetWrapper = kafkaAktivitetWrapper(mockBruker.fnrAsFnr)
            .copy(messageId = null, aktivitetskortType = AktivitetskortType.VARIG_LONNSTILSKUDD)
        val record = ConsumerRecord(
            topic,
            0,
            0,
            aktivitetWrapper.aktivitetskort.id.toString(),
            JsonUtils.toJson(aktivitetWrapper)
        )
        aktivitetskortConsumer!!.consume(record)
        assertFeilmeldingPublished(
            aktivitetWrapper.getAktivitetskortId(),
            DeserialiseringsFeil::class.java
        )
    }

    @Test
    fun should_handle_messageId_in_header() {
        var aktivitetWrapper = kafkaAktivitetWrapper(mockBruker.fnrAsFnr)
        aktivitetWrapper = aktivitetWrapper.copy(
            aktivitetskortType = AktivitetskortType.VARIG_LONNSTILSKUDD,
            messageId = null
        )
        val messageId = UUID.randomUUID()
        val record = ProducerRecord(
            topic,
            0,
            Date().time,
            aktivitetWrapper.aktivitetskort.id.toString(),
            JsonUtils.toJson(aktivitetWrapper)
        )
        val msgIdHeader: Header =
            RecordHeader(AktivitetskortConsumer.UNIQUE_MESSAGE_IDENTIFIER, messageId.toString().toByteArray())
        record.headers().add(msgIdHeader)
        val sendResult = aktivitetTestService.opprettEksterntAktivitetsKort(record)
        assertThat(sendResult.recordMetadata.hasOffset()).isTrue()
    }

    @Test
    fun should_throw_exception_when_messageId_is_equal_to_funksjonell_id() {
        val funksjonellId = UUID.randomUUID()
        val actual = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
        val wrapperDTO = KafkaAktivitetskortWrapperDTO(
            messageId = funksjonellId,
            aktivitetskortType = AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD,
//            actionType = ActionType.UPSERT_AKTIVITETSKORT_V1,
            aktivitetskort = actual,
            source = "source")
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(wrapperDTO))
        assertFeilmeldingPublished(funksjonellId, MessageIdIkkeUnikFeil::class.java)
    }

    @Test
    fun should_not_commit_database_transaction_if_runtimeException_is_thrown2() {
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
        val tiltaksaktivitetOppdatert: Aktivitetskort = aktivitetskort(funksjonellId, AktivitetStatus.AVBRUTT)
            .copy(detaljer = listOf(
                Attributt(
                    "Tiltaksnavn",
                    "Nytt navn"
                )
            ))
        val aktivitetskort = AktivitetskortTestBuilder.aktivitetskortMelding(tiltaksaktivitet)
        val aktivitetskortOppdatert = AktivitetskortTestBuilder.aktivitetskortMelding(tiltaksaktivitetOppdatert)
        val h1 = RecordHeader(
            AktivitetsbestillingCreator.HEADER_EKSTERN_REFERANSE_ID,
            ArenaId("ARENATA123").id().toByteArray()
        )
        val h2 = RecordHeader(AktivitetsbestillingCreator.HEADER_EKSTERN_ARENA_TILTAKSKODE, "MIDLONS".toByteArray())
        val record = ConsumerRecord(topic, 0, 0, funksjonellId.toString(), JsonUtils.toJson(aktivitetskort))
        record.headers().add(h1)
        record.headers().add(h2)
        val recordOppdatert =
            ConsumerRecord(topic, 0, 1, funksjonellId.toString(), JsonUtils.toJson(aktivitetskortOppdatert))
        recordOppdatert.headers().add(h1)
        recordOppdatert.headers().add(h2)

        /* Call consume directly to avoid retry / waiting for message to be consumed */aktivitetskortConsumer!!.consume(
            record
        )

        /* Simulate technical error after DETALJER_ENDRET processing */

        Mockito.doThrow(IllegalStateException("Ikke lov"))
            .`when`(aktivitetskortService)!!.oppdaterStatus(any(), any())
//            .`when`(aktivitetskortService?.oppdaterStatus(any<AktivitetData>(), any<AktivitetData>()))
        assertThrows(IllegalStateException::class.java) {
            aktivitetskortConsumer!!.consume(recordOppdatert)
        }

        /* Assert successful rollback */
        assertThat(messageDAO!!.exist(aktivitetskortOppdatert.messageId!!))
            .isFalse()
        val aktivitet = hentAktivitet(funksjonellId)
        assertThat(aktivitet.eksternAktivitet.detaljer[0].verdi).isEqualTo(
            tiltaksaktivitet.detaljer!![0].verdi
        )
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.PLANLAGT)
    }

    @Test
    fun aktivitet_med_fho_for_migrering_skal_ha_fho_etter_migrering() {
        val arenaaktivitetId = "ARENATA123"
        val arenaAktivitetDTO =
            aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, ArenaId(arenaaktivitetId), veileder)
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(tiltaksaktivitet),
            listOf(defaultcontext)
        )
        val aktivitet = hentAktivitet(funksjonellId)
        assertNotNull(aktivitet.forhaandsorientering)
        assertThat(aktivitet.endretAv).isEqualTo(tiltaksaktivitet.endretAv!!.ident)
        // Assert endreDato is now because we forhaandsorientering was created during test-run
        assertThat(DateUtils.dateToLocalDateTime(aktivitet.endretDato))
            .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(arenaAktivitetDTO.forhaandsorientering.id).isEqualTo(aktivitet.forhaandsorientering.id)
        assertThat(aktivitet.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.DETALJER_ENDRET)
    }

    @Autowired
    var sendBrukernotifikasjonCron: SendBrukernotifikasjonCron? = null
    @Test
    fun skal_migrere_eksisterende_forhaandorientering() {
        // Det finnes en arenaaktivtiet fra før
        val arenaaktivitetId = ArenaId("ARENATA123")
        // Opprett FHO på aktivitet
        aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, arenaaktivitetId, veileder)
        val record = brukernotifikasjonAsserts!!.assertOppgaveSendt(mockBruker.fnrAsFnr)
        // Migrer arenaaktivitet via topic
        val funksjonellId = UUID.randomUUID()
        val tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(tiltaksaktivitet),
            listOf(meldingContext(arenaaktivitetId))
        )
        // Bruker leser fho (POST på /lest)
        val aktivitet = hentAktivitet(funksjonellId)
        aktivitetTestService.lesFHO(mockBruker, aktivitet.id.toLong(), aktivitet.versjon.toLong())
        // Skal dukke opp Done melding på brukernotifikasjons-topic
        brukernotifikasjonAsserts!!.assertDone(record.key())
    }

    @Test
    fun tiltak_endepunkt_skal_legge_pa_aktivitet_id_og_versjon_pa_migrerte_arena_aktiviteteter() {
        val arenaaktivitetId = ArenaId("ARENATA123")
        val tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
        Mockito.`when`(unleashClient.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE))
            .thenReturn(false)
        val preMigreringArenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId)
        assertThat(preMigreringArenaAktiviteter).hasSize(1)
        assertThat(preMigreringArenaAktiviteter[0].id).isEqualTo(arenaaktivitetId.id()) // Skal være arenaid
        val context = meldingContext(arenaaktivitetId)
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(listOf(tiltaksaktivitet), listOf(context))
        Mockito.`when`(unleashClient.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)).thenReturn(true)
        val aktivitet = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter.stream().findFirst().get()
        val tekniskId = aktivitet.id
        val versjon = aktivitet.versjon.toLong()
        Mockito.`when`(unleashClient.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE))
            .thenReturn(false)
        val postMigreringArenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId)
        assertThat(postMigreringArenaAktiviteter).hasSize(1)
        val migrertArenaAktivitet = postMigreringArenaAktiviteter[0]
        assertThat(migrertArenaAktivitet.id).isEqualTo(tekniskId)
        assertThat(migrertArenaAktivitet.versjon).isEqualTo(versjon)
    }

    @Test
    fun skal_alltid_vere_like_mange_aktiviteter_med_toggle_av_eller_pa() {
        val arenaaktivitetId = ArenaId("ARENATA123")
        val tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)

        // Default toggle for testene er på
        Mockito.`when`(unleashClient.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE))
            .thenReturn(false)
        val preMigreringArenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId)
        assertThat(preMigreringArenaAktiviteter).hasSize(1)
        val preMigreringVeilarbAktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter
        assertThat(preMigreringVeilarbAktiviteter).isEmpty()

        // Migrer aktivtet
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(tiltaksaktivitet),
            listOf(defaultcontext)
        )
        val toggleAvArenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId)
        assertThat(toggleAvArenaAktiviteter).hasSize(1)
        val toggleAvVeilarbAktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter
        assertThat(toggleAvVeilarbAktiviteter).isEmpty()
        Mockito.`when`(unleashClient.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)).thenReturn(true)
        val togglePaArenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId)
        assertThat(togglePaArenaAktiviteter).isEmpty()
        val togglePaVeilarbAktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter
        assertThat(togglePaVeilarbAktiviteter).hasSize(1)
    }

    @Test
    fun skal_ikke_gi_ut_tiltakaktiviteter_pa_intern_api() {
        val tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(tiltaksaktivitet),
            listOf(defaultcontext)
        )
        val aktiviteter = aktivitetTestService.hentAktiviteterInternApi(veileder, mockBruker.aktorIdAsAktorId)
        assertThat(aktiviteter).isEmpty()
    }

    @Test
    fun skal_sette_nullable_felt_til_null() {
        val tiltaksaktivitet: Aktivitetskort = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
            .copy(sluttDato = null, startDato = null, beskrivelse = null)
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(tiltaksaktivitet),
            listOf(defaultcontext)
        )
        val aktivitet = hentAktivitet(tiltaksaktivitet.id)
        assertThat(aktivitet.fraDato).isNull()
        assertThat(aktivitet.tilDato).isNull()
        assertThat(aktivitet.beskrivelse).isNull()
    }

    @Test
    fun skal_lagre_riktig_identtype_pa_eksterne_aktiviteter() {
        val arbeidsgiverIdent = Ident("123456789", IdentType.ARBEIDSGIVER)
        val arbeidgiverAktivitet: Aktivitetskort = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
            .copy(endretAv = arbeidsgiverIdent)
        val tiltaksarragoerIdent = Ident("123456780", IdentType.TILTAKSARRANGOER)
        val tiltaksarrangoerAktivitet: Aktivitetskort = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
            .copy(endretAv = tiltaksarragoerIdent)
        val systemIdent = Ident("123456770", IdentType.SYSTEM)
        val systemAktivitetsKort: Aktivitetskort = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
            .copy(endretAv = systemIdent)
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(
                arbeidgiverAktivitet,
                tiltaksarrangoerAktivitet,
                systemAktivitetsKort
            ), listOf(defaultcontext, defaultcontext, defaultcontext)
        )
        val arbeidsAktivitet = hentAktivitet(arbeidgiverAktivitet.id)
        val tilatksarratgoerAktivitet = hentAktivitet(tiltaksarrangoerAktivitet.id)
        val systemAktivitet = hentAktivitet(systemAktivitetsKort.id)
        assertThat(arbeidsAktivitet.endretAv).isEqualTo(arbeidsgiverIdent.ident)
        assertThat(arbeidsAktivitet.endretAvType).isEqualTo(arbeidsgiverIdent.identType.toString())
        assertThat(tilatksarratgoerAktivitet.endretAv).isEqualTo(tiltaksarragoerIdent.ident)
        assertThat(tilatksarratgoerAktivitet.endretAvType)
            .isEqualTo(tiltaksarragoerIdent.identType.toString())
        assertThat(systemAktivitet.endretAv).isEqualTo(systemIdent.ident)
        assertThat(systemAktivitet.endretAvType).isEqualTo(systemIdent.identType.toInnsender().toString())
    }

    @Test
    fun skal_vise_lonnstilskudd_men_ikke_migrerte_arena_aktiviteter_hvis_toggle_er_av() {
        val arenaAktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
        val arenata123 = ArenaId("ARENATA123")
        val kontekst = meldingContext(arenata123, "MIDLONNTIL")
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(listOf(arenaAktivitet), listOf(kontekst))
        val midlertidigLonnstilskudd = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
        val kafkaAktivitetskortWrapperDTO = AktivitetskortTestBuilder.aktivitetskortMelding(
            midlertidigLonnstilskudd, UUID.randomUUID(), "TEAM_TILTAK", AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD
        )
        aktivitetTestService.opprettEksterntAktivitetsKort(listOf(kafkaAktivitetskortWrapperDTO))
        Mockito.`when`(unleashClient.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE)).thenReturn(true)
        val alleEksternAktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker)
        assertThat(alleEksternAktiviteter.aktiviteter).hasSize(2)
        Mockito.`when`(unleashClient.isEnabled(MigreringService.VIS_MIGRERTE_ARENA_AKTIVITETER_TOGGLE))
            .thenReturn(false)
        val eksterneAktiviteterUnntattMigrerteArenaAktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker)
        assertThat(eksterneAktiviteterUnntattMigrerteArenaAktiviteter.aktiviteter).hasSize(1)
        val aktivitet = eksterneAktiviteterUnntattMigrerteArenaAktiviteter.getAktiviteter()[0]
        assertThat(aktivitet.eksternAktivitet.type).isEqualTo(AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD)
    }

    @Test
    fun skal_kunne_kassere_aktiviteter() {
        val tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(tiltaksaktivitet),
            listOf(defaultcontext)
        )
        val kassering = KasseringsBestilling(
            "team-tiltak",
            UUID.randomUUID(),
//            ActionType.KASSER_AKTIVITET,
            NavIdent.of("z123456"),
            NorskIdent.of("12121212121"),
            tiltaksaktivitet.id,
            "Fordi"
        )
        aktivitetTestService.kasserEskterntAktivitetskort(kassering)
        val aktivitet = aktivitetTestService.hentAktivitetByFunksjonellId(mockBruker, veileder, kassering.aktivitetsId)
        assertThat(aktivitet.status).isEqualTo(AktivitetStatus.AVBRUTT)
        assertThat(aktivitet.endretAv).isEqualTo("z123456")
        assertThat(aktivitet.eksternAktivitet.detaljer).isEmpty()
        assertThat(aktivitet.eksternAktivitet.etiketter).isEmpty()
        assertThat(aktivitet.eksternAktivitet.handlinger).isEmpty()
        assertThat(aktivitet.eksternAktivitet.oppgave).isNull()
        val params = MapSqlParameterSource().addValue("aktivitetId", aktivitet.id)
        val count = namedParameterJdbcTemplate.queryForObject(
            """
                SELECT count(*)
                FROM KASSERT_AKTIVITET 
                WHERE AKTIVITET_ID = :aktivitetId
                
                """.trimIndent(), params, Int::class.javaPrimitiveType
        )
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun skal_kunne_publisere_feilmelding_nar_man_kasserer_aktivitet_som_ikke_finnes() {
        val funksjonellId = UUID.randomUUID()
        val kassering = KasseringsBestilling(
            "team-tiltak",
            UUID.randomUUID(),
//            ActionType.KASSER_AKTIVITET,
            NavIdent.of("z123456"),
            NorskIdent.of("12121212121"),
            funksjonellId,
            "Fordi"
        )
        aktivitetTestService.kasserEskterntAktivitetskort(kassering)
        assertFeilmeldingPublished(
            funksjonellId,
            AktivitetIkkeFunnetFeil::class.java
        )
    }

    @Test
    fun `relast skal overskrive første feilede migrering`() {
        val arenaaktivitetId = ArenaId("TA31212")
        val arenaAktivitetDTO =
            aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, arenaaktivitetId, veileder)
        val funksjonellId = UUID.randomUUID()
        val kontekst = meldingContext(arenaaktivitetId, "ARENA_TILTAK")
        val opprettet = ZonedDateTime.now().minusDays(10)
        val gammel = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
            .copy(endretTidspunkt = opprettet)
        val ny = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
            .copy(endretTidspunkt = ZonedDateTime.now())
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(gammel), listOf(kontekst))
        val initiellAktivitet = hentAktivitet(funksjonellId)
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(
            listOf(ny), listOf(kontekst))
        val versjoner = aktivitetTestService.hentVersjoner(initiellAktivitet.id, mockBruker, mockBruker)
        // Skal bare finnes siste versjon, alle andre skal slettes
        assertThat(versjoner).hasSize(1)
        val sisteVersjon = versjoner.first()
        assertThat(sisteVersjon.versjon.toLong()).isGreaterThan(initiellAktivitet.versjon.toLong())
        // Skal bruke første opprettet dato
        assertThat(DateUtils.dateToZonedDateTime(sisteVersjon.opprettetDato)).isCloseTo(opprettet, within(1, ChronoUnit.SECONDS))
        assertThat(sisteVersjon.transaksjonsType).isEqualTo(AktivitetTransaksjonsType.OPPRETTET)
        // TODO: Vurder om nyeste periode er mer riktig enn gammel periode
        assertThat(sisteVersjon.oppfolgingsperiodeId).isEqualTo(initiellAktivitet.oppfolgingsperiodeId)
        assertThat(sisteVersjon.isHistorisk).isEqualTo(initiellAktivitet.isHistorisk)
        // Behold FHO
        assertThat(arenaAktivitetDTO.forhaandsorientering.id).isEqualTo(sisteVersjon.forhaandsorientering.id)
    }

    private val mockBruker = MockNavService.createHappyBruker()
    private val veileder = MockNavService.createVeileder(mockBruker)
    private val endretDato = ZonedDateTime.now()
}
