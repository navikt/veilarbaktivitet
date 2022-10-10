package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.TiltakDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.mock_nav_modell.WireMockUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.test.context.junit4.SpringRunner;
import shaded.com.google.common.collect.Streams;

import java.time.*;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import static no.nav.veilarbaktivitet.aktivitetskort.IdentType.ARENAIDENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getRecords;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;


public class AktivitetskortConsumerIntegrationTest extends SpringBootTestBase {

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

    Consumer<String, String> aktivitetskortFeilConsumer;

    @Before
    public void cleanupBetweenTests() {
        aktivitetskortFeilConsumer = kafkaTestService.createStringStringConsumer(aktivitetskortFeilTopic);
    }

    TiltaksaktivitetDTO tiltaksaktivitetDTO(UUID funksjonellId, AktivitetStatus aktivitetStatus) {
        return TiltaksaktivitetDTO.builder()
                .id(funksjonellId)
                .eksternReferanseId(new Random().nextLong() + "")
                .personIdent(mockBruker.getFnr())
                .startDato(LocalDate.now().minusDays(30))
                .sluttDato(LocalDate.now().minusDays(30))
                .tittel("The Elder Scrolls: Arena")
                .beskrivelse("arenabeskrivelse")
                .aktivitetStatus(aktivitetStatus)
                .endretAv(new IdentDTO("arenaEndretav", ARENAIDENT))
                .endretDato(endretDato)
                .tiltaksNavn("Arendal")
                .tiltaksKode("Arenatiltakskode")
                .arrangoernavn("Arenaarrangørnavn")
                .deltakelseStatus("SOKT_INN")
                .detalj("deltakelsesprosent", "40")
                .detalj("dagerPerUke", "2")
                .build();
    }

    KafkaAktivitetWrapperDTO aktivitetskortMelding(TiltaksaktivitetDTO payload) {
        return aktivitetskortMelding(payload, UUID.randomUUID());
    }

    KafkaAktivitetWrapperDTO aktivitetskortMelding(TiltaksaktivitetDTO payload, UUID messageId) {
        return KafkaTiltaksAktivitet.builder()
                .messageId(messageId)
                .source("ARENA_TILTAK_AKTIVITET_ACL")
                .sendt(LocalDateTime.now())
                .actionType(ActionType.UPSERT_TILTAK_AKTIVITET_V1)
                .payload(payload)
                .build();
    }

    void sendOgVentPåTiltak(List<TiltaksaktivitetDTO> meldinger) {
        var aktivitetskorter = meldinger.stream().map(this::aktivitetskortMelding).toList();
        sendOgVentPåMeldinger(aktivitetskorter);
    }

    void sendOgVentPåMeldinger(List<KafkaAktivitetWrapperDTO> meldinger) {
        var lastrecord = Streams.mapWithIndex(meldinger.stream(),
                (aktivitetskort, index) -> new ProducerRecord<>(topic, aktivitetskort.funksjonellId().toString(), JsonUtils.toJson(aktivitetskort)))
            .map((record) -> producerClient.sendSync(record))
            .skip(meldinger.size() - 1)
            .findFirst().get();

        Awaitility.await().atMost(Duration.ofSeconds(1000))
            .until(() -> kafkaTestService.erKonsumert(topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, lastrecord.offset()));
    }

    @Test
    public void happy_case_upsert_ny_tiltaksaktivitet() {
        UUID funksjonellId = UUID.randomUUID();

        TiltaksaktivitetDTO tiltaksaktivitet = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT);
        sendOgVentPåTiltak(List.of(tiltaksaktivitet));

        var aktivitet = aktivitetTestService.hentAktiviteterForFnr(mockBruker)
                .aktiviteter.stream()
                .filter((a) -> Objects.equals(a.getFunksjonellId(), funksjonellId))
                .findFirst();
        Assertions.assertTrue(aktivitet.isPresent());

        Assertions.assertEquals(aktivitet.get().getStatus(), AktivitetStatus.PLANLAGT);
        Assertions.assertEquals(aktivitet.get().getTiltak(), new TiltakDTO(
                tiltaksaktivitet.getTiltaksNavn(),
                tiltaksaktivitet.getArrangoernavn(),
                tiltaksaktivitet.getDeltakelseStatus(),
                Integer.parseInt(tiltaksaktivitet.getDetaljer().get("dagerPerUke")),
                Integer.parseInt(tiltaksaktivitet.getDetaljer().get("deltakelsesprosent"))
        ));

    }

    @Test
    public void happy_case_upsert_status_existing_tiltaksaktivitet() {
        UUID funksjonellId = UUID.randomUUID();

        TiltaksaktivitetDTO tiltaksaktivitet = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT);
        TiltaksaktivitetDTO tiltaksaktivitetUpdate = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.GJENNOMFORES);

        sendOgVentPåTiltak(List.of(tiltaksaktivitet, tiltaksaktivitetUpdate));

        var aktivitet = aktivitetTestService.hentAktiviteterForFnr(mockBruker)
                .aktiviteter.stream()
                .filter((a) -> Objects.equals(a.getFunksjonellId(), funksjonellId))
                .findFirst();
        Assertions.assertTrue(aktivitet.isPresent());
        Assertions.assertEquals(AktivitetStatus.GJENNOMFORES, aktivitet.get().getStatus());
        Assertions.assertEquals(AktivitetTransaksjonsType.STATUS_ENDRET, aktivitet.get().getTransaksjonsType());
    }

    @Test
    public void duplikat_melding_bare_1_opprettet_transaksjon() {
        UUID funksjonellId = UUID.randomUUID();

        TiltaksaktivitetDTO tiltaksaktivitetDTO = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT);

        KafkaAktivitetWrapperDTO kafkaAktivitetWrapperDTO =  aktivitetskortMelding(tiltaksaktivitetDTO);

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, "1", JsonUtils.toJson(kafkaAktivitetWrapperDTO));
        RecordMetadata recordMetadata = producerClient.sendSync(producerRecord);
        RecordMetadata recordMetadataDuplicate = producerClient.sendSync(producerRecord);
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> kafkaTestService.erKonsumert(topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, recordMetadataDuplicate.offset()));

        var aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker)
                .aktiviteter.stream()
                .filter((a) -> Objects.equals(a.getFunksjonellId(), funksjonellId))
                .toList();
        Assertions.assertEquals(aktiviteter.size(), 1);
        Assertions.assertEquals(AktivitetTransaksjonsType.OPPRETTET, aktiviteter.stream().findFirst().get().getTransaksjonsType() );
    }

    @Test
    public void oppdatering_av_detaljer_gir_riktig_transaksjon() {
        UUID funksjonellId = UUID.randomUUID();

        TiltaksaktivitetDTO tiltaksaktivitet = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT);
        TiltaksaktivitetDTO tiltaksaktivitetEndret = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT)
                .withTiltaksNavn("Nytt navn");

        sendOgVentPåTiltak(List.of(tiltaksaktivitet, tiltaksaktivitetEndret));

        var aktivitet = aktivitetTestService.hentAktiviteterForFnr(mockBruker)
                .aktiviteter.stream()
                .filter((a) -> Objects.equals(a.getFunksjonellId(), funksjonellId))
                .findFirst();
        Assertions.assertEquals(AktivitetTransaksjonsType.DETALJER_ENDRET, aktivitet.get().getTransaksjonsType());
    }

    @Test
    public void oppdatering_status_og_detaljer_gir_2_transaksjoner() {
        UUID funksjonellId = UUID.randomUUID();

        TiltaksaktivitetDTO tiltaksaktivitet = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT);
        TiltaksaktivitetDTO tiltaksaktivitetEndret = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.GJENNOMFORES)
                .withDeltakelseStatus("FÅTT_PLASS");

        sendOgVentPåTiltak(List.of(tiltaksaktivitet, tiltaksaktivitetEndret));

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

        TiltaksaktivitetDTO tiltaksaktivitetDTO = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT)
                .withEndretDato(endretDato);

        sendOgVentPåTiltak(List.of(tiltaksaktivitetDTO));

        var aktivitet = hentAktivitet(funksjonellId);
        Instant endretDatoInstant = endretDato.atZone(ZoneId.systemDefault()).toInstant();
        assertThat(aktivitet.getEndretDato()).isEqualTo(endretDatoInstant);

    }

    @Test
    public void skal_handtere_gamle_meldinger_etter_ny_melding() {
        var funksjonellId = UUID.randomUUID();
        var nyesteNavn = "Nytt navn";

        var tiltaksaktivitet = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT)
                .withTiltaksNavn("Gammelt navn");
        var tiltaksMelding = aktivitetskortMelding(tiltaksaktivitet);
        var tiltaksaktivitetEndret = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT)
                .withTiltaksNavn(nyesteNavn);
        var tiltaksMeldingEndret = aktivitetskortMelding(tiltaksaktivitetEndret);

        sendOgVentPåMeldinger(List.of(tiltaksMelding, tiltaksMeldingEndret, tiltaksMelding));

        var aktivitet = hentAktivitet(funksjonellId);
        assertThat(aktivitet.getTiltak().tiltaksnavn()).isEqualTo(nyesteNavn);

    }

    private AktivitetDTO hentAktivitet(UUID funksjonellId) {
        return aktivitetTestService.hentAktiviteterForFnr(mockBruker)
            .aktiviteter.stream()
            .filter((a) -> Objects.equals(a.getFunksjonellId(), funksjonellId))
            .findFirst().get();
    }

    @Test
    public void avbrutt_aktivitet_kan_ikke_endres() {
        var funksjonellId = UUID.randomUUID();

        var tiltaksaktivitet = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.AVBRUTT);
        var tiltaksaktivitetEndret = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT);

        sendOgVentPåTiltak(List.of(tiltaksaktivitet, tiltaksaktivitetEndret));

        var aktivitet = hentAktivitet(funksjonellId);
        assertThat(aktivitet.getStatus()).isEqualTo(AktivitetStatus.AVBRUTT);
    }

    @Test
    public void fullført_aktivitet_kan_ikke_endres() {
        var funksjonellId = UUID.randomUUID();

        var tiltaksaktivitet = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.FULLFORT);
        var tiltaksaktivitetEndret = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT);

        sendOgVentPåTiltak(List.of(tiltaksaktivitet, tiltaksaktivitetEndret));

        var aktivitet = hentAktivitet(funksjonellId);
        assertThat(aktivitet.getStatus()).isEqualTo(AktivitetStatus.FULLFORT);

        var singleRecord = getSingleRecord(aktivitetskortFeilConsumer, aktivitetskortFeilTopic, 10000);
        assertThat(singleRecord.key()).isEqualTo(funksjonellId.toString());
    }

    @Test
    public void aktivitet_kan_settes_til_avbrutt() {
        var funksjonellId = UUID.randomUUID();

        var tiltaksaktivitet = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT);
        var tiltaksaktivitetEndret = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.AVBRUTT);

        sendOgVentPåTiltak(List.of(tiltaksaktivitet, tiltaksaktivitetEndret));

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

        RecordMetadata lastRecordMetadata = messages.stream()
                .map(pair -> new ProducerRecord<>(topic, pair.messageId().toString(), pair.json()))
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

        TiltaksaktivitetDTO tiltaksaktivitet = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT);
        sendOgVentPåTiltak(List.of(tiltaksaktivitet));

        var singleRecord = getSingleRecord(aktivitetskortFeilConsumer, aktivitetskortFeilTopic, 10000);
        var payload = JsonUtils.fromJson(singleRecord.value(), AktivitetskortFeilMelding.class);
        assertThat(singleRecord.key()).isEqualTo(funksjonellId.toString());
        assertThat(payload.errorMessage()).isEqualTo(String.format("class no.nav.veilarbaktivitet.aktivitetskort.UgyldigIdentFeil AktørId ikke funnet for fnr :%s", mockBruker.getFnr()));
    }

    @Test
    public void should_not_commit_database_transaction_if_runtimeException_is_thrown2() {
        UUID funksjonellId = UUID.randomUUID();
        TiltaksaktivitetDTO tiltaksaktivitet = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT);
        TiltaksaktivitetDTO tiltaksaktivitetOppdatert = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.AVBRUTT)
            .withTiltaksNavn("Nytt navn");
        var aktivitetskort = aktivitetskortMelding(tiltaksaktivitet);
        var aktivitetskortOppdatert = aktivitetskortMelding(tiltaksaktivitetOppdatert);

        var record = new ConsumerRecord<>(topic, 0, 0, funksjonellId.toString(), JsonUtils.toJson(aktivitetskort));
        var recordOppdatert = new ConsumerRecord<>(topic, 0, 1, funksjonellId.toString(), JsonUtils.toJson(aktivitetskortOppdatert));

        /* Call consume directly to avoid retry / waiting for message to be consumed */
        aktivitetskortConsumer.consume(record);

        /* Simulate technical error after DETALJER_ENDRET processing */
        doThrow(new IllegalStateException("Ikke lov")).when(aktivitetskortService).oppdaterStatus(any(), any());
        Assertions.assertThrows(IllegalStateException.class, () -> aktivitetskortConsumer.consume(recordOppdatert));

        /* Assert successful rollback */
        assertThat(messageDAO.exist(aktivitetskortOppdatert.messageId)).isFalse();
        var aktivitet = hentAktivitet(funksjonellId);
         assertThat(aktivitet.getTiltak().tiltaksnavn()).isEqualTo(tiltaksaktivitet.getTiltaksNavn());
        assertThat(aktivitet.getStatus()).isEqualTo(AktivitetStatus.PLANLAGT);
    }

    @Test
    public void new_aktivitet_with_existing_forhaandsorientering_should_have_forhaandsorientering() {
        String arenaaktivitetId = "ARENA123";
        ArenaAktivitetDTO arenaAktivitetDTO = aktivitetTestService.opprettFHO(mockBruker, new ArenaId(arenaaktivitetId));

        UUID funksjonellId = UUID.randomUUID();

        TiltaksaktivitetDTO tiltaksaktivitet = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT)
                .withEksternReferanseId("123");
        sendOgVentPåTiltak(List.of(tiltaksaktivitet));

        var aktivitet = aktivitetTestService.hentAktiviteterForFnr(mockBruker)
                .aktiviteter.stream()
                .filter((a) -> Objects.equals(a.getFunksjonellId(), funksjonellId))
                .findFirst();
        Assertions.assertTrue(aktivitet.isPresent());

        Assertions.assertEquals(aktivitet.get().getStatus(), AktivitetStatus.PLANLAGT);
        Assertions.assertEquals(aktivitet.get().getTiltak(), new TiltakDTO(
                tiltaksaktivitet.getTiltaksNavn(),
                tiltaksaktivitet.getArrangoernavn(),
                tiltaksaktivitet.getDeltakelseStatus(),
                Integer.parseInt(tiltaksaktivitet.getDetaljer().get("dagerPerUke")),
                Integer.parseInt(tiltaksaktivitet.getDetaljer().get("deltakelsesprosent"))
        ));

    }

    private final MockBruker mockBruker = MockNavService.createHappyBruker();
    private final LocalDateTime endretDato = LocalDateTime.now().minusDays(100);
}