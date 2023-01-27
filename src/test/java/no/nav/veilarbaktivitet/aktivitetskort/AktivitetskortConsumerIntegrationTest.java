package no.nav.veilarbaktivitet.aktivitetskort;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.domain.Ident;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.EksternAktivitetDTO;
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortFeilMelding;
import no.nav.veilarbaktivitet.aktivitetskort.dto.Attributt;
import no.nav.veilarbaktivitet.aktivitetskort.dto.Etikett;
import no.nav.veilarbaktivitet.aktivitetskort.dto.IdentType;
import no.nav.veilarbaktivitet.aktivitetskort.feil.UgyldigIdentFeil;
import no.nav.veilarbaktivitet.aktivitetskort.feil.UlovligEndringFeil;
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDto;
import no.nav.veilarbaktivitet.aktivitetskort.service.AktivitetskortService;
import no.nav.veilarbaktivitet.aktivitetskort.service.TiltakMigreringCronService;
import no.nav.veilarbaktivitet.aktivitetskort.service.UpsertActionResult;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAsserts;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonAssertsConfig;
import no.nav.veilarbaktivitet.brukernotifikasjon.varsel.SendBrukernotifikasjonCron;
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder;
import no.nav.veilarbaktivitet.mock_nav_modell.WireMockUtil;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.testutils.AktivitetskortTestBuilder;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static no.nav.veilarbaktivitet.aktivitetskort.AktivitetsbestillingCreator.HEADER_EKSTERN_ARENA_TILTAKSKODE;
import static no.nav.veilarbaktivitet.aktivitetskort.AktivitetsbestillingCreator.HEADER_EKSTERN_REFERANSE_ID;
import static no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMetrikker.AKTIVITETSKORT_UPSERT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getRecords;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;


class AktivitetskortConsumerIntegrationTest extends SpringBootTestBase {

    @Autowired
    UnleashClient unleashClient;

    @Autowired
    KafkaProducerClient<String, String> producerClient;

    @Autowired
    AktivitetskortConsumer aktivitetskortConsumer;

    @Autowired
    AktivitetsMessageDAO messageDAO;

    @Autowired
    AktivitetskortService aktivitetskortService;

    @Autowired
    TiltakMigreringCronService tiltakMigreringCronService;

    @Autowired
    MeterRegistry meterRegistry;

    @Value("${topic.inn.aktivitetskort}")
    String topic;

    @Value("${topic.ut.aktivitetskort-feil}")
    String aktivitetskortFeilTopic;
    @Value("${topic.inn.oppfolgingsperiode}")
    String oppfolgingperiodeTopic;

    @Value("${topic.ut.aktivitetskort-idmapping}")
    String aktivitetskortIdMappingTopic;

    @Value("${spring.kafka.consumer.group-id}")
    String springKafkaConsumerGroupId;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;

    BrukernotifikasjonAsserts brukernotifikasjonAsserts;
    Consumer<String, String> aktivitetskortFeilConsumer;
    Consumer<String, String> aktivitetskortIdMappingConsumer;

    @BeforeEach
    public void cleanupBetweenTests() {
        when(unleashClient.isEnabled(MigreringService.EKSTERN_AKTIVITET_TOGGLE)).thenReturn(true);
        aktivitetskortFeilConsumer = kafkaTestService.createStringStringConsumer(aktivitetskortFeilTopic);
        aktivitetskortIdMappingConsumer = kafkaTestService.createStringStringConsumer(aktivitetskortIdMappingTopic);
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
    }

    ArenaMeldingHeaders meldingContext() {
        return new ArenaMeldingHeaders(new ArenaId("ARENATA123"), "MIDL");
    }
    ArenaMeldingHeaders meldingContext(ArenaId eksternRefanseId) {
        return new ArenaMeldingHeaders(eksternRefanseId, "MIDL");
    }
    ArenaMeldingHeaders meldingContext(ArenaId eksternRefanseId, String arenaTiltakskode) {
        return new ArenaMeldingHeaders(eksternRefanseId, arenaTiltakskode);
    }

    private final ArenaMeldingHeaders defaultcontext = meldingContext();
    Aktivitetskort aktivitetskort(UUID funksjonellId, AktivitetStatus aktivitetStatus) {
        return AktivitetskortTestBuilder.ny(
                funksjonellId,
                aktivitetStatus,
                endretDato,
                mockBruker
        );
    }

    private void assertFeilmeldingPublished(UUID funksjonellId, Class<? extends Exception> errorClass) {
        var singleRecord = getSingleRecord(aktivitetskortFeilConsumer, aktivitetskortFeilTopic, 10000);
        var payload = JsonUtils.fromJson(singleRecord.value(), AktivitetskortFeilMelding.class);
        assertThat(singleRecord.key()).isEqualTo(funksjonellId.toString());
        assertThat(payload.errorMessage()).contains(errorClass.getName());
    }

    private void assertIdMappingPublished(UUID funksjonellId, ArenaId arenaId) {
        var singleRecord = getSingleRecord(aktivitetskortIdMappingConsumer, aktivitetskortIdMappingTopic, 10000);
        var payload = JsonUtils.fromJson(singleRecord.value(), IdMappingDto.class);
        assertThat(singleRecord.key()).isEqualTo(funksjonellId.toString());
        assertThat(payload.arenaId()).isEqualTo(arenaId);
    }

    @Test
    void happy_case_upsert_ny_arenatiltaksaktivitet() {
        //trenges for og teste med count hvis ikke må man også matche på tags for å få testet counten
        //burde man endre på metrikkene her? kan man vite en fulstendig liste av aktiviteskort og skilde?
        meterRegistry.find(AKTIVITETSKORT_UPSERT).meters().forEach(it -> meterRegistry.remove(it));
        UUID funksjonellId = UUID.randomUUID();

        Aktivitetskort actual = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        ArenaId arenata123 = new ArenaId("ARENATA123");
        ArenaMeldingHeaders kontekst = meldingContext(arenata123, "MIDL");
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(actual), List.of(kontekst));

        var count = meterRegistry.find(AKTIVITETSKORT_UPSERT).counter().count();

        assertThat(count).isEqualTo(1.0);

        var aktivitet = hentAktivitet(funksjonellId);

        assertThat(aktivitet.getType()).isEqualTo(AktivitetTypeDTO.EKSTERNAKTIVITET);

        Assertions.assertEquals(AktivitetTransaksjonsType.OPPRETTET, aktivitet.getTransaksjonsType());
        Assertions.assertEquals(AktivitetStatus.PLANLAGT, aktivitet.getStatus());
        Assertions.assertTrue(aktivitet.isAvtalt());
        Assertions.assertEquals(Innsender.ARENAIDENT.name(), aktivitet.getEndretAvType());
        Assertions.assertEquals(aktivitet.getEksternAktivitet(), new EksternAktivitetDTO(
                AktivitetskortType.ARENA_TILTAK,
                null,
                Collections.emptyList(),
                actual.detaljer,
                actual.etiketter
        ));
        assertIdMappingPublished(funksjonellId, arenata123);
    }

    @Test
    void aktiviteter_opprettet_av_bruker_skal_ha_riktig_endretAv_verdi() {
        var brukerIdent = "12129312122";
        var aktivitetskort = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
                .withEndretAv(new Ident(
                    brukerIdent,
                    IdentType.PERSONBRUKERIDENT
                ));
        var kafkaAktivitetskortWrapperDTO = AktivitetskortTestBuilder.aktivitetskortMelding(
                aktivitetskort, UUID.randomUUID(), "TEAM_TILTAK", AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD);
        aktivitetTestService.opprettEksterntAktivitetsKort(List.of(kafkaAktivitetskortWrapperDTO));
        var resultat = hentAktivitet(aktivitetskort.getId());
        assertThat(resultat.getEndretAv()).isEqualTo(brukerIdent);
        assertThat(resultat.getEndretAvType()).isEqualTo(Innsender.BRUKER.toString());
    }


    @Test
    void ekstern_aktivitet_skal_ha_oppfolgingsperiode() {
        UUID funksjonellId = UUID.randomUUID();

        Aktivitetskort actual = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        KafkaAktivitetskortWrapperDTO wrapperDTO = KafkaAktivitetskortWrapperDTO.builder()
                .aktivitetskortType(AktivitetskortType.MIDLERTIDIG_LONNSTILSKUDD)
                .actionType(ActionType.UPSERT_AKTIVITETSKORT_V1)
                .aktivitetskort(actual)
                .source("source")
                .messageId(UUID.randomUUID())
                .build();

        aktivitetTestService.opprettEksterntAktivitetsKort(List.of(wrapperDTO));

        var aktivitet = hentAktivitet(funksjonellId);

        Assertions.assertEquals(mockBruker.getOppfolgingsperiode(), aktivitet.getOppfolgingsperiodeId());
    }

    @Test
    void historisk_arenatiltak_aktivitet_skal_ha_oppfolgingsperiode() {
        UUID funksjonellId = UUID.randomUUID();

        Aktivitetskort actual = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        actual.setEndretTidspunkt(ZonedDateTime.now().minusDays(75));
        ArenaId arenata123 = new ArenaId("ARENATA123");
        ArenaMeldingHeaders kontekst = meldingContext(arenata123, "MIDLONNTIL");
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(actual), List.of(kontekst));
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(actual), List.of(kontekst)); // Kjør to ganger for å sjekke at vi tar med oss OPPRETTET_SOM_HISTORISK når vi oppdaterer

        var aktivitetFoer = hentAktivitet(funksjonellId);

        assertThat(aktivitetFoer.getOppfolgingsperiodeId()).isNotNull();
        assertThat(aktivitetFoer.isHistorisk()).isFalse();

        var aktivitetFoerOpprettetSomHistorisk = jdbcTemplate.queryForObject("""
                SELECT opprettet_som_historisk 
                FROM EKSTERNAKTIVITET
                WHERE AKTIVITET_ID = ? 
                ORDER BY VERSJON desc
                FETCH NEXT 1 ROW ONLY 
                """, boolean.class, aktivitetFoer.getId());

        assertThat(aktivitetFoerOpprettetSomHistorisk).isTrue();

        tiltakMigreringCronService.settTiltakOpprettetSomHistoriskTilHistorisk();

        var aktivitetEtter = hentAktivitet(funksjonellId);

        assertThat(aktivitetEtter.isHistorisk()).isTrue();
    }

    @Test
    void arenatiltak_uten_oppfolgingsperiode_skal_ignoreres() {
        UUID funksjonellId = UUID.randomUUID();

        Aktivitetskort actual = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        actual.setEndretTidspunkt(ZonedDateTime.now().minusDays(200));
        ArenaId arenaId = new ArenaId("ARENATA123");
        ArenaMeldingHeaders kontekst = meldingContext(arenaId, "MIDLONNTIL");
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(actual), List.of(kontekst));

        // Aktivitet skal ikke bli opprettet
        var aktivitet = hentAktivitet(funksjonellId);
        assertThat(aktivitet).isNull();

        var upsertActionResult = jdbcTemplate.queryForObject("""
                SELECT ACTION_RESULT
                FROM AKTIVITETSKORT_MSG_ID
                WHERE FUNKSJONELL_ID = ?
                """, String.class, funksjonellId.toString());

        assertThat(upsertActionResult).isEqualTo(UpsertActionResult.IGNORE.name());
    }

    @Test
    void happy_case_upsert_status_existing_tiltaksaktivitet() {
        UUID funksjonellId = UUID.randomUUID();

        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        ArenaMeldingHeaders meldingContext = meldingContext(new ArenaId("ARENATA123"), "MIDL");
        var annenVeileder = new Ident("ANNEN_NAV_IDENT", Innsender.ARENAIDENT);
        Aktivitetskort tiltaksaktivitetUpdate = aktivitetskort(funksjonellId, AktivitetStatus.GJENNOMFORES)
                .withEndretAv(annenVeileder);
        ArenaMeldingHeaders updatemeldingContext = meldingContext(new ArenaId("ARENATA123"), "MIDL");


        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(tiltaksaktivitet, tiltaksaktivitetUpdate), List.of(meldingContext, updatemeldingContext));

        AktivitetDTO aktivitet = hentAktivitet(funksjonellId);

        assertThat(aktivitet.getType()).isEqualTo(AktivitetTypeDTO.EKSTERNAKTIVITET);
        Assertions.assertNotNull(aktivitet);
        assertThat(tiltaksaktivitet.endretTidspunkt).isCloseTo(DateUtils.dateToZonedDateTime(aktivitet.getEndretDato()), within(1, ChronoUnit.MILLIS));
        assertThat(aktivitet.getEndretAv()).isEqualTo(annenVeileder.ident());
        Assertions.assertEquals(AktivitetStatus.GJENNOMFORES, aktivitet.getStatus());
        Assertions.assertEquals(AktivitetTransaksjonsType.STATUS_ENDRET, aktivitet.getTransaksjonsType());
    }

    @Test
    void duplikat_melding_bare_1_opprettet_transaksjon() {
        UUID funksjonellId = UUID.randomUUID();

        Aktivitetskort aktivitetskort = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        KafkaAktivitetskortWrapperDTO kafkaAktivitetskortWrapperDTO =  AktivitetskortTestBuilder.aktivitetskortMelding(aktivitetskort);
        var context = meldingContext();

        ProducerRecord<String, String> producerRecord = aktivitetTestService.makeAktivitetskortProducerRecord(kafkaAktivitetskortWrapperDTO, context);
        producerClient.sendSync(producerRecord);
        RecordMetadata recordMetadataDuplicate = producerClient.sendSync(producerRecord);
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> kafkaTestService.erKonsumert(topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, recordMetadataDuplicate.offset()));

        var aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker)
                .aktiviteter.stream()
                .filter((a) -> Objects.equals(a.getFunksjonellId(), funksjonellId))
                .toList();
        Assertions.assertEquals(1, aktiviteter.size());
        Assertions.assertEquals(AktivitetTransaksjonsType.OPPRETTET, aktiviteter.stream().findFirst().get().getTransaksjonsType() );
    }

    @Test
    void oppdatering_av_detaljer_gir_riktig_transaksjon() {
        UUID funksjonellId = UUID.randomUUID();

        var context = meldingContext();
        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        Aktivitetskort tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT).toBuilder()
                .detalj(new Attributt("Tiltaksnavn", "Nytt navn")).build();

        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(tiltaksaktivitet, tiltaksaktivitetEndret), List.of(context, context));

        var aktivitet = hentAktivitet(funksjonellId);
        Assertions.assertEquals(AktivitetTransaksjonsType.DETALJER_ENDRET, aktivitet.getTransaksjonsType());
    }

    @Test
    void oppdatering_status_og_detaljer_gir_4_transaksjoner() {
        UUID funksjonellId = UUID.randomUUID();

        var context = meldingContext();
        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
                .withAvtaltMedNav(false);
        var etikett = new Etikett("FÅTT_PLASS");
        Aktivitetskort tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.GJENNOMFORES).toBuilder()
                .avtaltMedNav(true)
                .etikett(etikett)
                .build();

        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(tiltaksaktivitet, tiltaksaktivitetEndret), List.of(context, context));

        var aktivitetId = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter.stream()
                .findFirst().get().getId();
        var aktivitetVersjoner = aktivitetTestService.hentVersjoner(aktivitetId, mockBruker, mockBruker);

        Assertions.assertEquals(4, aktivitetVersjoner.size());
        var sisteVersjon = aktivitetVersjoner.get(0);
        assertThat(sisteVersjon.isAvtalt()).isTrue();
        assertThat(sisteVersjon.getEksternAktivitet().etiketter()).isEqualTo(tiltaksaktivitetEndret.etiketter);
        assertThat(sisteVersjon.getStatus()).isEqualTo(AktivitetStatus.GJENNOMFORES);
        Assertions.assertEquals(AktivitetTransaksjonsType.STATUS_ENDRET, aktivitetVersjoner.get(0).getTransaksjonsType());
        Assertions.assertEquals(AktivitetTransaksjonsType.DETALJER_ENDRET, aktivitetVersjoner.get(1).getTransaksjonsType());
        Assertions.assertEquals(AktivitetTransaksjonsType.AVTALT, aktivitetVersjoner.get(2).getTransaksjonsType());
        Assertions.assertEquals(AktivitetTransaksjonsType.OPPRETTET, aktivitetVersjoner.get(3).getTransaksjonsType());
    }

    @Test
    void endretTidspunkt_skal_settes_fra_melding() {
        UUID funksjonellId = UUID.randomUUID();

        var context = meldingContext();
        Aktivitetskort aktivitetskort = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
                .withEndretTidspunkt(endretDato);

        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(aktivitetskort), List.of(context));

        var aktivitet = hentAktivitet(funksjonellId);
        Instant endretDatoInstant = endretDato.toInstant();
        assertThat(aktivitet.getEndretDato()).isEqualTo(endretDatoInstant);
    }

    @Test
    void skal_skippe_gamle_meldinger_etter_ny_melding() {
        var funksjonellId = UUID.randomUUID();
        var nyesteNavn = "Nytt navn";

        var context = meldingContext();
        var tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
                .toBuilder()
                .detalj(new Attributt("Tiltaksnavn", "Gammelt navn"))
                .build();
        var tiltaksMelding = AktivitetskortTestBuilder.aktivitetskortMelding(tiltaksaktivitet);
        var tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
                .toBuilder()
                .detalj(new Attributt("Tiltaksnavn", nyesteNavn))
                .build();
        var tiltaksMeldingEndret = AktivitetskortTestBuilder.aktivitetskortMelding(tiltaksaktivitetEndret);

        aktivitetTestService.opprettEksterntAktivitetsKort(List.of(tiltaksMelding, tiltaksMeldingEndret, tiltaksMelding), List.of(context, context, context));

        var aktivitet = hentAktivitet(funksjonellId);
        var detaljer = aktivitet.getEksternAktivitet().detaljer();
        assertThat(detaljer.stream().filter(it -> it.label().equals("Tiltaksnavn"))).hasSize(1);
        assertThat(detaljer).containsOnlyOnceElementsOf(List.of(new Attributt("Tiltaksnavn", nyesteNavn)));
    }

    private AktivitetDTO hentAktivitet(UUID funksjonellId) {
        return aktivitetTestService.hentAktiviteterForFnr(mockBruker, veileder)
            .aktiviteter.stream()
            .filter((a) -> Objects.equals(a.getFunksjonellId(), funksjonellId))
            .findFirst()
            .orElse(null);
    }

    @Test
    void avbrutt_aktivitet_kan_ikke_endres() {
        var funksjonellId = UUID.randomUUID();

        var tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.AVBRUTT);
        var tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);

        var context = meldingContext();
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(tiltaksaktivitet, tiltaksaktivitetEndret), List.of(context, context));

        var aktivitet = hentAktivitet(funksjonellId);
        assertThat(aktivitet.getStatus()).isEqualTo(AktivitetStatus.AVBRUTT);
    }

    @Test
    void fullfort_aktivitet_kan_ikke_endres() {
        var funksjonellId = UUID.randomUUID();

        var tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.FULLFORT);
        var tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);

        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(tiltaksaktivitet, tiltaksaktivitetEndret), List.of(defaultcontext, defaultcontext));

        var aktivitet = hentAktivitet(funksjonellId);
        assertThat(aktivitet.getStatus()).isEqualTo(AktivitetStatus.FULLFORT);

        assertFeilmeldingPublished(
                funksjonellId,
                UlovligEndringFeil.class
        );
    }

    @Test
    void aktivitet_kan_settes_til_avbrutt() {
        var funksjonellId = UUID.randomUUID();

        var tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        var tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.AVBRUTT);

        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(tiltaksaktivitet, tiltaksaktivitetEndret), List.of(defaultcontext, defaultcontext));

        var aktivitet = hentAktivitet(funksjonellId);
        assertThat(aktivitet.getStatus()).isEqualTo(AktivitetStatus.AVBRUTT);
    }


    @Test
    void invalid_messages_should_catch_deserializer_error() {
        List<AktivitetskortProducerUtil.Pair> messages = List.of(
                AktivitetskortProducerUtil.missingFieldRecord(mockBruker.getFnrAsFnr()),
                AktivitetskortProducerUtil.extraFieldRecord(mockBruker.getFnrAsFnr()),
                AktivitetskortProducerUtil.invalidDateFieldRecord(mockBruker.getFnrAsFnr())
        );

        Iterable<Header> headers = List.of(
                new RecordHeader(HEADER_EKSTERN_REFERANSE_ID, new ArenaId("ARENATA123").id().getBytes()),
                new RecordHeader(HEADER_EKSTERN_ARENA_TILTAKSKODE, "MIDLONS".getBytes())
        );
        RecordMetadata lastRecordMetadata = messages.stream()
                .map(pair -> new ProducerRecord<>(topic, null, pair.messageId().toString(), pair.json(), headers))
                .map(record -> producerClient.sendSync(record))
                .reduce((first, second) -> second)
                .get();

        Awaitility.await().atMost(Duration.ofSeconds(1000))
                .until(() -> kafkaTestService.erKonsumert(topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, lastRecordMetadata.offset()));

        ConsumerRecords<String, String> records = getRecords(aktivitetskortFeilConsumer, 1000, messages.size());

        assertThat(records.count()).isEqualTo(messages.size());
    }

    @Test
    void should_catch_ugyldigident_error() {
        WireMockUtil.aktorUtenGjeldende(mockBruker.getFnr(), mockBruker.getAktorId());

        UUID funksjonellId = UUID.randomUUID();

        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(tiltaksaktivitet), List.of(defaultcontext));

        assertFeilmeldingPublished(
            funksjonellId,
            UgyldigIdentFeil.class
        );
    }

    @Test
    void should_not_commit_database_transaction_if_runtimeException_is_thrown2() {
        UUID funksjonellId = UUID.randomUUID();
        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        Aktivitetskort tiltaksaktivitetOppdatert = aktivitetskort(funksjonellId, AktivitetStatus.AVBRUTT)
                .toBuilder()
                .detalj(new Attributt("Tiltaksnavn", "Nytt navn"))
                .build();
        var aktivitetskort = AktivitetskortTestBuilder.aktivitetskortMelding(tiltaksaktivitet);
        var aktivitetskortOppdatert = AktivitetskortTestBuilder.aktivitetskortMelding(tiltaksaktivitetOppdatert);

        var h1 = new RecordHeader(HEADER_EKSTERN_REFERANSE_ID, new ArenaId("ARENATA123").id().getBytes());
        var h2 = new RecordHeader(HEADER_EKSTERN_ARENA_TILTAKSKODE, "MIDLONS".getBytes());

        var record = new ConsumerRecord<>(topic, 0, 0, funksjonellId.toString(), JsonUtils.toJson(aktivitetskort));
        record.headers().add(h1);
        record.headers().add(h2);
        var recordOppdatert = new ConsumerRecord<>(topic, 0, 1, funksjonellId.toString(), JsonUtils.toJson(aktivitetskortOppdatert));
        recordOppdatert.headers().add(h1);
        recordOppdatert.headers().add(h2);

        /* Call consume directly to avoid retry / waiting for message to be consumed */
        aktivitetskortConsumer.consume(record);

        /* Simulate technical error after DETALJER_ENDRET processing */
        doThrow(new IllegalStateException("Ikke lov")).when(aktivitetskortService).oppdaterStatus(any(), any());
        Assertions.assertThrows(IllegalStateException.class, () -> aktivitetskortConsumer.consume(recordOppdatert));

        /* Assert successful rollback */
        assertThat(messageDAO.exist(aktivitetskortOppdatert.messageId)).isFalse();
        var aktivitet = hentAktivitet(funksjonellId);
        assertThat(aktivitet.getEksternAktivitet().detaljer().get(0).verdi()).isEqualTo(tiltaksaktivitet.detaljer.get(0).verdi());
        assertThat(aktivitet.getStatus()).isEqualTo(AktivitetStatus.PLANLAGT);
    }

    @Test
    void new_aktivitet_with_existing_forhaandsorientering_should_have_forhaandsorientering() {
        String arenaaktivitetId = "ARENATA123";
        ArenaAktivitetDTO arenaAktivitetDTO = aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, new ArenaId(arenaaktivitetId), veileder);

        UUID funksjonellId = UUID.randomUUID();

        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(tiltaksaktivitet), List.of(defaultcontext));

        var aktivitet = hentAktivitet(funksjonellId);

        Assertions.assertNotNull(aktivitet.getForhaandsorientering());
        assertThat(aktivitet.getEndretAv()).isEqualTo(veileder.getNavIdent());
        // Assert endreDato is now because we forhaandsorientering was created during test-run
        assertThat(DateUtils.dateToLocalDateTime(aktivitet.getEndretDato())).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));
        assertThat(arenaAktivitetDTO.getForhaandsorientering().getId()).isEqualTo(aktivitet.getForhaandsorientering().getId());
        assertThat(aktivitet.getTransaksjonsType()).isEqualTo(AktivitetTransaksjonsType.DETALJER_ENDRET);
    }

    @Autowired
    SendBrukernotifikasjonCron sendBrukernotifikasjonCron;

    @Test
    void skal_migrere_eksisterende_forhaandorientering() {
        // Det finnes en arenaaktivtiet fra før
        var arenaaktivitetId = new ArenaId("ARENATA123");
        // Opprett FHO på aktivitet
        aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, arenaaktivitetId, veileder);
        var record = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr());
        // Migrer arenaaktivitet via topic
        UUID funksjonellId = UUID.randomUUID();
        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(tiltaksaktivitet), List.of(meldingContext(arenaaktivitetId)));
        // Bruker leser fho (POST på /lest)
        var aktivitet = hentAktivitet(funksjonellId);
        aktivitetTestService.lesFHO(mockBruker, Long.parseLong(aktivitet.getId()), Long.parseLong(aktivitet.getVersjon()));
        // Skal dukke opp Done melding på brukernotifikasjons-topic
        brukernotifikasjonAsserts.assertDone(record.key());
    }

    @Test
    void tiltak_endepunkt_skal_legge_pa_aktivitet_id_pa_migrerte_arena_aktiviteteter() {
        ArenaId arenaaktivitetId =  new ArenaId("ARENATA123");
        Aktivitetskort tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT);

        when(unleashClient.isEnabled(MigreringService.EKSTERN_AKTIVITET_TOGGLE)).thenReturn(false);

        var preMigreringArenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId);
        assertThat(preMigreringArenaAktiviteter).hasSize(1);
        assertThat(preMigreringArenaAktiviteter.get(0).getId()).isEqualTo(arenaaktivitetId.id()); // Skal være arenaid

        var context = meldingContext(arenaaktivitetId);
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(tiltaksaktivitet), List.of(context));

        var arenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId);
        assertThat(arenaAktiviteter).hasSize(1);
        assertThat(arenaAktiviteter.get(0).getId()).doesNotStartWith("ARENA"); // Skal ikke være arenaId
    }

    @Test
    void skal_ikke_gi_ut_tiltakaktiviteter_pa_intern_api() {
        Aktivitetskort tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT);
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(tiltaksaktivitet), List.of(defaultcontext));
        var aktiviteter = aktivitetTestService.hentAktiviteterInternApi(veileder, mockBruker.getAktorIdAsAktorId());
        assertThat(aktiviteter).isEmpty();
    }

    @Test
    void skal_sette_nullable_felt_til_null() {
        Aktivitetskort tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
            .withSluttDato(null)
            .withStartDato(null)
            .withBeskrivelse(null);
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(tiltaksaktivitet), List.of(defaultcontext));
        var aktivitet = hentAktivitet(tiltaksaktivitet.getId());
        assertThat(aktivitet.getFraDato()).isNull();
        assertThat(aktivitet.getTilDato()).isNull();
        assertThat(aktivitet.getBeskrivelse()).isNull();
    }

    @Test
    void skal_lagre_riktig_identtype_pa_eksterne_aktiviteter() {
        var arbeidsgiverIdent = new Ident("123456789", IdentType.ARBEIDSGIVER);
        Aktivitetskort arbeidgiverAktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
                .withEndretAv(arbeidsgiverIdent);
        var tiltaksarragoerIdent = new Ident("123456780", IdentType.TILTAKSARRANGOER);
        Aktivitetskort tiltaksarrangoerAktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
                .withEndretAv(tiltaksarragoerIdent);
        var systemIdent = new Ident("123456770", IdentType.SYSTEM);
        Aktivitetskort systemAktivitetsKort = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT)
                .withEndretAv(systemIdent);
        aktivitetTestService.opprettEksterntAktivitetsKortByAktivitetkort(List.of(arbeidgiverAktivitet, tiltaksarrangoerAktivitet, systemAktivitetsKort), List.of(defaultcontext, defaultcontext, defaultcontext));
        var arbeidsAktivitet = hentAktivitet(arbeidgiverAktivitet.getId());
        var tilatksarratgoerAktivitet = hentAktivitet(tiltaksarrangoerAktivitet.getId());
        var systemAktivitet = hentAktivitet(systemAktivitetsKort.getId());
        assertThat(arbeidsAktivitet.getEndretAv()).isEqualTo(arbeidsgiverIdent.ident());
        assertThat(arbeidsAktivitet.getEndretAvType()).isEqualTo(arbeidsgiverIdent.identType().toString());
        assertThat(tilatksarratgoerAktivitet.getEndretAv()).isEqualTo(tiltaksarragoerIdent.ident());
        assertThat(tilatksarratgoerAktivitet.getEndretAvType()).isEqualTo(tiltaksarragoerIdent.identType().toString());
        assertThat(systemAktivitet.getEndretAv()).isEqualTo(systemIdent.ident());
        assertThat(systemAktivitet.getEndretAvType()).isEqualTo(systemIdent.identType().toInnsender().toString());
    }



    private final MockBruker mockBruker = MockNavService.createHappyBruker();
    private final MockVeileder veileder = MockNavService.createVeileder(mockBruker);
    private final ZonedDateTime endretDato = ZonedDateTime.now().minusDays(100);
}
