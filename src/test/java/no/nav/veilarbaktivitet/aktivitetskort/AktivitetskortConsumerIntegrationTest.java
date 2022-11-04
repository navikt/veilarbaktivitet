package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.EksternAktivitetDTO;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.feil.UgyldigIdentFeil;
import no.nav.veilarbaktivitet.aktivitetskort.feil.UlovligEndringFeil;
import no.nav.veilarbaktivitet.aktivitetskort.service.AktivitetskortService;
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
import no.nav.veilarbaktivitet.oppfolging.siste_periode.SisteOppfolgingsperiodeV1;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import shaded.com.google.common.collect.Streams;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static no.nav.veilarbaktivitet.aktivitetskort.IdentType.ARENAIDENT;
import static no.nav.veilarbaktivitet.aktivitetskort.MeldingContext.HEADER_EKSTERN_ARENA_TILTAKSKODE;
import static no.nav.veilarbaktivitet.aktivitetskort.MeldingContext.HEADER_EKSTERN_REFERANSE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getRecords;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;


public class AktivitetskortConsumerIntegrationTest extends SpringBootTestBase {

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

    @Value("${topic.inn.aktivitetskort}")
    String topic;

    @Value("${topic.ut.aktivitetskort-feil}")
    String aktivitetskortFeilTopic;
    @Value("${topic.inn.oppfolgingsperiode}")
    String oppfolgingperiodeTopic;
    @Value("${spring.kafka.consumer.group-id}")
    String springKafkaConsumerGroupId;

    @Autowired
    BrukernotifikasjonAssertsConfig brukernotifikasjonAssertsConfig;
    BrukernotifikasjonAsserts brukernotifikasjonAsserts;
    Consumer<String, String> aktivitetskortFeilConsumer;

    @Before
    public void cleanupBetweenTests() {
        when(unleashClient.isEnabled(MigreringService.EKSTERN_AKTIVITET_TOGGLE)).thenReturn(true);
        aktivitetskortFeilConsumer = kafkaTestService.createStringStringConsumer(aktivitetskortFeilTopic);
        brukernotifikasjonAsserts = new BrukernotifikasjonAsserts(brukernotifikasjonAssertsConfig);
    }

    MeldingContext meldingContext() {
        return new MeldingContext(new ArenaId("123"), "MIDL", AktivitetsbestillingCreator.ARENA_TILTAK_AKTIVITET_ACL, AktivitetskortType.ARENA_TILTAK);
    }
    MeldingContext meldingContext(ArenaId eksternRefanseId) {
        return new MeldingContext(eksternRefanseId, "MIDL", AktivitetsbestillingCreator.ARENA_TILTAK_AKTIVITET_ACL, AktivitetskortType.ARENA_TILTAK);
    }
    MeldingContext meldingContext(ArenaId eksternRefanseId, String arenaTiltakskode) {
        return new MeldingContext(eksternRefanseId, arenaTiltakskode, AktivitetsbestillingCreator.ARENA_TILTAK_AKTIVITET_ACL, AktivitetskortType.ARENA_TILTAK);
    }

    private MeldingContext defaultcontext = meldingContext();
    Aktivitetskort aktivitetskort(UUID funksjonellId, AktivitetStatus aktivitetStatus) {
        return Aktivitetskort.builder()
                .id(funksjonellId)
                .personIdent(mockBruker.getFnr())
                .startDato(LocalDate.now().minusDays(30))
                .sluttDato(LocalDate.now().minusDays(30))
                .tittel("The Elder Scrolls: Arena")
                .beskrivelse("arenabeskrivelse")
                .aktivitetStatus(aktivitetStatus)
                .endretAv(new IdentDTO("arenaEndretav", ARENAIDENT))
                .endretTidspunkt(endretDato)
                .etikett(new Etikett("SOKT_INN"))
                .detalj(new Attributt("arrangørnavn", "Arendal"))
                .detalj(new Attributt("deltakelsesprosent", "40%"))
                .detalj(new Attributt("dager per uke", "2"))
                .build();
    }

    KafkaAktivitetskortWrapperDTO aktivitetskortMelding(Aktivitetskort payload) {
        return aktivitetskortMelding(payload, UUID.randomUUID());
    }

    KafkaAktivitetskortWrapperDTO aktivitetskortMelding(Aktivitetskort payload, UUID messageId) {
        return KafkaAktivitetskortWrapperDTO.builder()
                .messageId(messageId)
                .source("ARENA_TILTAK_AKTIVITET_ACL")
                .actionType(ActionType.UPSERT_AKTIVITETSKORT_V1)
                .aktivitetskort(payload)
                .aktivitetskortType(AktivitetskortType.ARENA_TILTAK)
                .build();
    }

    void sendOgVentPåTiltak(List<Aktivitetskort> meldinger, List<MeldingContext> meldingContextList) {
        var aktivitetskorter = meldinger.stream().map(this::aktivitetskortMelding).toList();
        sendOgVentPåMeldinger(aktivitetskorter, meldingContextList);
    }

    void sendOgVentPåMeldinger(List<KafkaAktivitetskortWrapperDTO> meldinger, List<MeldingContext> contexts) {
        Assertions.assertEquals(meldinger.size(), contexts.size());

        var lastRecord = Streams.mapWithIndex(meldinger.stream(),
                (melding, index) -> makeProducerRecord(melding, contexts.get((int) index)))
                .map((record) -> producerClient.sendSync(record))
                .skip(meldinger.size() - 1)
                .findFirst().get();

        Awaitility.await().atMost(Duration.ofSeconds(500))
            .until(() -> kafkaTestService.erKonsumert(topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, lastRecord.offset()));
    }



    private ProducerRecord makeProducerRecord(KafkaAktivitetskortWrapperDTO melding, MeldingContext context) {
        List<Header> headers = List.of(
                new RecordHeader(HEADER_EKSTERN_REFERANSE_ID, context.eksternReferanseId().id().getBytes()),
                new RecordHeader(HEADER_EKSTERN_ARENA_TILTAKSKODE, context.arenaTiltakskode().getBytes())
        );
        return new ProducerRecord<>(topic, null, melding.aktivitetskort.getId().toString(), JsonUtils.toJson(melding), headers);
    }

    private void assertFeilmeldingPublished(UUID funksjonellId, Class<? extends Exception> errorClass) {
        var singleRecord = getSingleRecord(aktivitetskortFeilConsumer, aktivitetskortFeilTopic, 10000);
        var payload = JsonUtils.fromJson(singleRecord.value(), AktivitetskortFeilMelding.class);
        assertThat(singleRecord.key()).isEqualTo(funksjonellId.toString());
        assertThat(payload.errorMessage()).contains(errorClass.getName());
    }

    @Test
    public void happy_case_upsert_ny_tiltaksaktivitet() {
        UUID funksjonellId = UUID.randomUUID();

        Aktivitetskort actual = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        MeldingContext kontekst = meldingContext(new ArenaId("123"), "MIDL");
        sendOgVentPåTiltak(List.of(actual), List.of(kontekst));

        var aktivitet = hentAktivitet(funksjonellId);

        assertThat(aktivitet.getType()).isEqualTo(AktivitetTypeDTO.EKSTERNAKTIVITET);

        Assertions.assertEquals(AktivitetStatus.PLANLAGT, aktivitet.getStatus());
        Assertions.assertEquals(aktivitet.getEksternAktivitet(), new EksternAktivitetDTO(
                AktivitetskortType.ARENA_TILTAK,
                null,
                Collections.emptyList(),
                actual.detaljer,
                actual.etiketter
        ));

    }

    @Test
    public void happy_case_upsert_status_existing_tiltaksaktivitet() {
        UUID funksjonellId = UUID.randomUUID();

        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        MeldingContext meldingContext = meldingContext(new ArenaId("123"), "MIDL");
        Aktivitetskort tiltaksaktivitetUpdate = aktivitetskort(funksjonellId, AktivitetStatus.GJENNOMFORES);
        MeldingContext updatemeldingContext = meldingContext(new ArenaId("123"), "MIDL");


        sendOgVentPåTiltak(List.of(tiltaksaktivitet, tiltaksaktivitetUpdate), List.of(meldingContext, updatemeldingContext));

        AktivitetDTO aktivitet = hentAktivitet(funksjonellId);

        assertThat(aktivitet.getType()).isEqualTo(AktivitetTypeDTO.EKSTERNAKTIVITET);
        Assertions.assertNotNull(aktivitet);
        assertThat(tiltaksaktivitet.endretTidspunkt).isCloseTo(DateUtils.dateToLocalDateTime(aktivitet.getEndretDato()), within(1, ChronoUnit.MILLIS));
        Assertions.assertEquals(tiltaksaktivitet.endretAv.ident(), aktivitet.getEndretAv());
        Assertions.assertEquals(AktivitetStatus.GJENNOMFORES, aktivitet.getStatus());
        Assertions.assertEquals(AktivitetTransaksjonsType.STATUS_ENDRET, aktivitet.getTransaksjonsType());
    }

    @Test
    public void duplikat_melding_bare_1_opprettet_transaksjon() {
        UUID funksjonellId = UUID.randomUUID();

        Aktivitetskort aktivitetskort = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        KafkaAktivitetskortWrapperDTO kafkaAktivitetskortWrapperDTO =  aktivitetskortMelding(aktivitetskort);
        var context = meldingContext();

        ProducerRecord<String, String> producerRecord = makeProducerRecord(kafkaAktivitetskortWrapperDTO, context);
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
    public void oppdatering_av_detaljer_gir_riktig_transaksjon() {
        UUID funksjonellId = UUID.randomUUID();

        var context = meldingContext();
        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        Aktivitetskort tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT).toBuilder()
                .detalj(new Attributt("Tiltaksnavn", "Nytt navn")).build();

        sendOgVentPåTiltak(List.of(tiltaksaktivitet, tiltaksaktivitetEndret), List.of(context, context));

        var aktivitet = hentAktivitet(funksjonellId);
        Assertions.assertEquals(AktivitetTransaksjonsType.DETALJER_ENDRET, aktivitet.getTransaksjonsType());
    }

    @Test
    public void oppdatering_status_og_detaljer_gir_2_transaksjoner() {
        UUID funksjonellId = UUID.randomUUID();

        var context = meldingContext();
        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        Aktivitetskort tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.GJENNOMFORES).toBuilder()
                .etikett(new Etikett("FÅTT_PLASS"))
                .build();

        sendOgVentPåTiltak(List.of(tiltaksaktivitet, tiltaksaktivitetEndret), List.of(context, context));

        var aktivitetId = aktivitetTestService.hentAktiviteterForFnr(mockBruker).aktiviteter.stream()
                .findFirst().get().getId();
        var aktivitet = aktivitetTestService.hentVersjoner(aktivitetId, mockBruker, mockBruker);

        Assertions.assertEquals(3, aktivitet.size());
        Assertions.assertEquals(AktivitetTransaksjonsType.STATUS_ENDRET, aktivitet.get(0).getTransaksjonsType());
        Assertions.assertEquals(AktivitetTransaksjonsType.DETALJER_ENDRET, aktivitet.get(1).getTransaksjonsType());
        Assertions.assertEquals(AktivitetTransaksjonsType.OPPRETTET, aktivitet.get(2).getTransaksjonsType());
    }

    @Test
    public void endretTidspunkt_skal_settes_fra_melding() {
        UUID funksjonellId = UUID.randomUUID();

        var context = meldingContext();
        Aktivitetskort aktivitetskort = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
                .withEndretTidspunkt(endretDato);

        sendOgVentPåTiltak(List.of(aktivitetskort), List.of(context));

        var aktivitet = hentAktivitet(funksjonellId);
        Instant endretDatoInstant = endretDato.atZone(ZoneId.systemDefault()).toInstant();
        assertThat(aktivitet.getEndretDato()).isEqualTo(endretDatoInstant);

    }

    @Test
    public void skal_skippe_gamle_meldinger_etter_ny_melding() {
        var funksjonellId = UUID.randomUUID();
        var nyesteNavn = "Nytt navn";

        var context = meldingContext();
        var tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
                .toBuilder()
                .detalj(new Attributt("Tiltaksnavn", "Gammelt navn"))
                .build();
        var tiltaksMelding = aktivitetskortMelding(tiltaksaktivitet);
        var tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT)
                .toBuilder()
                .detalj(new Attributt("Tiltaksnavn", nyesteNavn))
                .build();
        var tiltaksMeldingEndret = aktivitetskortMelding(tiltaksaktivitetEndret);

        sendOgVentPåMeldinger(List.of(tiltaksMelding, tiltaksMeldingEndret, tiltaksMelding), List.of(context, context, context));

        var aktivitet = hentAktivitet(funksjonellId);
        var detaljer = aktivitet.getEksternAktivitet().detaljer();
        assertThat(detaljer.stream().filter(it -> it.label().equals("Tiltaksnavn"))).hasSize(1);
        assertThat(detaljer).containsOnlyOnceElementsOf(List.of(new Attributt("Tiltaksnavn", nyesteNavn)));
    }

    private AktivitetDTO hentAktivitet(UUID funksjonellId) {
        return aktivitetTestService.hentAktiviteterForFnr(mockBruker, veileder)
            .aktiviteter.stream()
            .filter((a) -> Objects.equals(a.getFunksjonellId(), funksjonellId))
            .findFirst().get();
    }

    @Test
    public void avbrutt_aktivitet_kan_ikke_endres() {
        var funksjonellId = UUID.randomUUID();

        var tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.AVBRUTT);
        var tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);

        var context = meldingContext();
        sendOgVentPåTiltak(List.of(tiltaksaktivitet, tiltaksaktivitetEndret), List.of(context, context));

        var aktivitet = hentAktivitet(funksjonellId);
        assertThat(aktivitet.getStatus()).isEqualTo(AktivitetStatus.AVBRUTT);
    }

    @Test
    public void fullført_aktivitet_kan_ikke_endres() {
        var funksjonellId = UUID.randomUUID();

        var tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.FULLFORT);
        var tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);

        sendOgVentPåTiltak(List.of(tiltaksaktivitet, tiltaksaktivitetEndret), List.of(defaultcontext, defaultcontext));

        var aktivitet = hentAktivitet(funksjonellId);
        assertThat(aktivitet.getStatus()).isEqualTo(AktivitetStatus.FULLFORT);

        assertFeilmeldingPublished(
                funksjonellId,
                UlovligEndringFeil.class
        );
    }

    @Test
    public void aktivitet_kan_settes_til_avbrutt() {
        var funksjonellId = UUID.randomUUID();

        var tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        var tiltaksaktivitetEndret = aktivitetskort(funksjonellId, AktivitetStatus.AVBRUTT);

        sendOgVentPåTiltak(List.of(tiltaksaktivitet, tiltaksaktivitetEndret), List.of(defaultcontext, defaultcontext));

        var aktivitet = hentAktivitet(funksjonellId);
        assertThat(aktivitet.getStatus()).isEqualTo(AktivitetStatus.AVBRUTT);
    }


    @Test
    public void invalid_messages_should_catch_deserializer_error() {
        List<AktivitetskortProducerUtil.Pair> messages = List.of(
                AktivitetskortProducerUtil.missingFieldRecord(),
                AktivitetskortProducerUtil.extraFieldRecord(),
                AktivitetskortProducerUtil.invalidDateFieldRecord()
        );

        Iterable<Header> headers = List.of(
                new RecordHeader(HEADER_EKSTERN_REFERANSE_ID, new ArenaId("123").id().getBytes()),
                new RecordHeader(HEADER_EKSTERN_ARENA_TILTAKSKODE, "MIDLONS".getBytes())
        );
        RecordMetadata lastRecordMetadata = messages.stream()
                .map(pair -> new ProducerRecord<>(topic, null, pair.messageId().toString(), pair.json(), headers))
                .map(record -> producerClient.sendSync(record))
                .reduce((first, second) -> second)
                .get();

        Awaitility.await().atMost(Duration.ofSeconds(1000))
                .until(() -> kafkaTestService.erKonsumert(topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, lastRecordMetadata.offset()));

        ConsumerRecords<String, String> records = getRecords(aktivitetskortFeilConsumer, 10000, messages.size());

        assertThat(records.count()).isEqualTo(messages.size());
    }

    @Test
    public void should_catch_ugyldigident_error() {
        WireMockUtil.aktorUtenGjeldende(mockBruker.getFnr(), mockBruker.getAktorId());

        UUID funksjonellId = UUID.randomUUID();

        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        sendOgVentPåTiltak(List.of(tiltaksaktivitet), List.of(defaultcontext));

        assertFeilmeldingPublished(
            funksjonellId,
            UgyldigIdentFeil.class
        );
    }

    @Test
    public void should_not_commit_database_transaction_if_runtimeException_is_thrown2() {
        UUID funksjonellId = UUID.randomUUID();
        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        Aktivitetskort tiltaksaktivitetOppdatert = aktivitetskort(funksjonellId, AktivitetStatus.AVBRUTT)
                .toBuilder()
                .detalj(new Attributt("Tiltaksnavn", "Nytt navn"))
                .build();
        var aktivitetskort = aktivitetskortMelding(tiltaksaktivitet);
        var aktivitetskortOppdatert = aktivitetskortMelding(tiltaksaktivitetOppdatert);

        var h1 = new RecordHeader(HEADER_EKSTERN_REFERANSE_ID, new ArenaId("123").id().getBytes());
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
    public void new_aktivitet_with_existing_forhaandsorientering_should_have_forhaandsorientering() {
        String arenaaktivitetId = "ARENA123";
        ArenaAktivitetDTO arenaAktivitetDTO = aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, new ArenaId(arenaaktivitetId), veileder);

        UUID funksjonellId = UUID.randomUUID();

        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        sendOgVentPåTiltak(List.of(tiltaksaktivitet), List.of(defaultcontext));

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
    public void skal_migrere_eksisterende_forhaandorientering() {
        // Det finnes en arenaaktivtiet fra før
        var arenaaktivitetId = new ArenaId("123");
        // Opprett FHO på aktivitet
        ArenaAktivitetDTO arenaAktivitetDTO = aktivitetTestService.opprettFHOForArenaAktivitet(mockBruker, arenaaktivitetId, veileder);
        var record = brukernotifikasjonAsserts.assertOppgaveSendt(mockBruker.getFnrAsFnr(), null);
        // Migrer arenaaktivitet via topic
        UUID funksjonellId = UUID.randomUUID();
        Aktivitetskort tiltaksaktivitet = aktivitetskort(funksjonellId, AktivitetStatus.PLANLAGT);
        sendOgVentPåTiltak(List.of(tiltaksaktivitet), List.of(meldingContext(arenaaktivitetId)));
        // Bruker leser fho (POST på /lest)
        var aktivitet = hentAktivitet(funksjonellId);
        aktivitetTestService.lesFHO(mockBruker, Long.parseLong(aktivitet.getId()), Long.parseLong(aktivitet.getVersjon()));
        // Skal dukke opp Done melding på brukernotifikasjons-topic
        brukernotifikasjonAsserts.assertDone(record.key());
    }

    @Test
    public void tiltak_endepunkt_skal_legge_pa_aktivitet_id_pa_migrerte_arena_aktiviteteter() {
        ArenaId arenaaktivitetId =  new ArenaId("123");
        Aktivitetskort tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT);

        when(unleashClient.isEnabled(MigreringService.EKSTERN_AKTIVITET_TOGGLE)).thenReturn(false);

        var preMigreringArenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId);
        assertThat(preMigreringArenaAktiviteter).hasSize(1);
        assertThat(preMigreringArenaAktiviteter.get(0).getId()).isEqualTo(arenaaktivitetId);
        assertThat(preMigreringArenaAktiviteter.get(0).getAktivitetId()).isNull();

        var context = meldingContext(arenaaktivitetId);
        sendOgVentPåTiltak(List.of(tiltaksaktivitet), List.of(context));

        var arenaAktiviteter = aktivitetTestService.hentArenaAktiviteter(mockBruker, arenaaktivitetId);
        assertThat(arenaAktiviteter).hasSize(1);
        assertThat(arenaAktiviteter.get(0).getId()).isEqualTo(arenaaktivitetId);
        assertThat(arenaAktiviteter.get(0).getAktivitetId()).isNotNull();
    }

    @Test
    public void skal_ikke_kunne_endre_historisk_aktivitet() {
        Aktivitetskort tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT);
        sendOgVentPåTiltak(List.of(tiltaksaktivitet), List.of(defaultcontext));
        avsluttOppfolgingsPeriode();
        sendOgVentPåTiltak(List.of(
            tiltaksaktivitet.withSluttDato(LocalDate.now())
        ), List.of(defaultcontext));
        assertFeilmeldingPublished(
            tiltaksaktivitet.id,
            UlovligEndringFeil.class
        );
    }

    private void avsluttOppfolgingsPeriode() {
        var now = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
        var start = now.withYear(2020);
        var key = mockBruker.getAktorId();
        var payload = JsonUtils.toJson(
            SisteOppfolgingsperiodeV1.builder()
                .aktorId(mockBruker.getAktorId())
                .uuid(mockBruker.getOppfolgingsperiode())
                .startDato(start)
                .sluttDato(now)
        );
        var recordMetatdata = producerClient.sendSync(new ProducerRecord<>(
            oppfolgingperiodeTopic,
            key,
            payload
        ));
        Awaitility.await().atMost(Duration.ofSeconds(5))
            .until(() -> kafkaTestService.erKonsumert(oppfolgingperiodeTopic, springKafkaConsumerGroupId, recordMetatdata.offset()));
    }

    @Test
    public void skal_ikke_gi_ut_tiltakaktiviteter_pa_intern_api() {
        Aktivitetskort tiltaksaktivitet = aktivitetskort(UUID.randomUUID(), AktivitetStatus.PLANLAGT);
        sendOgVentPåTiltak(List.of(tiltaksaktivitet), List.of(defaultcontext));
        var aktiviteter = aktivitetTestService.hentAktiviteterInternApi(veileder, mockBruker.getAktorIdAsAktorId());
        assertThat(aktiviteter).isEmpty();
    }


    private final MockBruker mockBruker = MockNavService.createHappyBruker();
    private final MockVeileder veileder = MockNavService.createVeileder(mockBruker);
    private final LocalDateTime endretDato = LocalDateTime.now().minusDays(100);
}
