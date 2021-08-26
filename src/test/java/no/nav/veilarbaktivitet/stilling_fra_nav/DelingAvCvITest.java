package no.nav.veilarbaktivitet.stilling_fra_nav;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.TilstandEnum;
import no.nav.veilarbaktivitet.nivaa4.Nivaa4Client;
import no.nav.veilarbaktivitet.oppfolging_status.OppfolgingStatusClient;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.Arbeidssted;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.util.MockBruker;
import no.nav.veilarbaktivitet.util.WireMockUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@EmbeddedKafka(topics = {"${topic.inn.stillingFraNav}","${topic.ut.stillingFraNav}"}, partitions = 1)
@AutoConfigureWireMock(port = 0)
@Slf4j
public class DelingAvCvITest {


    @Autowired
    EmbeddedKafkaBroker embeddedKafka;

    @Value("${topic.inn.stillingFraNav}")
    private String innTopic;

    @Value("${topic.ut.stillingFraNav}")
    private String utTopic;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    /***** Ekte bønner *****/

    @Autowired
    KafkaTemplate<String, ForesporselOmDelingAvCv> producer;

    @Autowired
    OpprettForesporselOmDelingAvCv service;

    @Autowired
    OppfolgingStatusClient oppfolgingStatusClient;

    @Autowired
    Nivaa4Client nivaa4Client;

    @After
    public void verify_no_unmatched() {
        assertTrue(WireMock.findUnmatchedRequests().isEmpty());
    }

    @Test
    public void happy_case() {
        final Consumer<String, DelingAvCvRespons> consumer = createConsumer();

        MockBruker mockBruker = MockBruker.happyBruker("1234", "4321");
        WireMockUtil.stubBruker(mockBruker);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.PROVER_VARSLING);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });
    }

    @Test
    public void ikke_under_oppfolging() {
        final Consumer<String, DelingAvCvRespons> consumer = createConsumer();

        MockBruker mockBruker = MockBruker.happyBruker("1234", "4321");
        mockBruker.setUnderOppfolging(false);
        WireMockUtil.stubBruker(mockBruker);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNull();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.IKKE_OPPRETTET);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });
    }

    @Test
    public void under_oppfolging_kvp() {
        final Consumer<String, DelingAvCvRespons> consumer = createConsumer();

        MockBruker mockBruker = MockBruker.happyBruker("1234", "4321");
        mockBruker.setUnderOppfolging(true);
        mockBruker.setErUnderKvp(true);
        WireMockUtil.stubBruker(mockBruker);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNull();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.IKKE_OPPRETTET);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

    }

    @Test
    public void under_manuell_oppfolging() {
        final Consumer<String, DelingAvCvRespons> consumer = createConsumer();

        MockBruker mockBruker = MockBruker.happyBruker("1234", "4321");
        mockBruker.setErManuell(true);
        WireMockUtil.stubBruker(mockBruker);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.IKKE_VARSLET);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

    }

    @Test
    public void reservert_i_krr() {
        final Consumer<String, DelingAvCvRespons> consumer = createConsumer();

        MockBruker mockBruker = MockBruker.happyBruker("1234", "4321");
        mockBruker.setErReservertKrr(true);
        WireMockUtil.stubBruker(mockBruker);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.IKKE_VARSLET);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

    }

    @Test
    public void mangler_nivaa4() {
        final Consumer<String, DelingAvCvRespons> consumer = createConsumer();

        MockBruker mockBruker = MockBruker.happyBruker("1234", "4321");
        mockBruker.setHarBruktNivaa4(false);
        WireMockUtil.stubBruker(mockBruker);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
        producer.send(innTopic, melding.getBestillingsId(), melding);

        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.IKKE_VARSLET);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

    }

    @Test
    public void duplikat_bestillingsId_ignoreres() {
        final Consumer<String, DelingAvCvRespons> consumer = createConsumer();

        MockBruker mockBruker = MockBruker.happyBruker("1234", "4321");
        WireMockUtil.stubBruker(mockBruker);

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId, mockBruker.getAktorId());
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(mockBruker.getAktorId());
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getTilstand()).isEqualTo(TilstandEnum.PROVER_VARSLING);
            assertions.assertThat(value.getSvar()).isNull();
            assertions.assertAll();
        });

        ForesporselOmDelingAvCv duplikatMelding = createMelding(bestillingsId, mockBruker.getAktorId());
        producer.send(innTopic, duplikatMelding.getBestillingsId(), duplikatMelding);
        Exception exception = assertThrows(IllegalStateException.class, () -> getSingleRecord(consumer, utTopic, 5000));
        assertEquals("No records found for topic", exception.getMessage());
    }

    static ForesporselOmDelingAvCv createMelding(String bestillingsId, String aktorId) {
        return ForesporselOmDelingAvCv.newBuilder()
                .setAktorId(aktorId)
                .setArbeidsgiver("arbeidsgiver")
                .setArbeidssteder(List.of(
                        Arbeidssted.newBuilder()
                                .setAdresse("adresse")
                                .setPostkode("1234")
                                .setKommune("kommune")
                                .setBy("by")
                                .setFylke("fylke")
                                .setLand("land").build(),
                        Arbeidssted.newBuilder()
                                .setAdresse("VillaRosa")
                                .setPostkode(null)
                                .setKommune(null)
                                .setBy(null)
                                .setFylke(null)
                                .setLand("spania").build()))
                .setBestillingsId(bestillingsId)
                .setOpprettet(Instant.now())
                .setOpprettetAv("Z999999")
                .setCallId("callId")
                .setSoknadsfrist("10102021")
                .setStillingsId("stillingsId")
                .setStillingstittel("stillingstittel")
                .setSvarfrist(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();
    }

    private Consumer<String, DelingAvCvRespons> createConsumer() {
        Consumer<String, DelingAvCvRespons> consumer = buildConsumer(
                StringDeserializer.class,
                KafkaAvroDeserializer.class
        );
        embeddedKafka.consumeFromEmbeddedTopics(consumer, utTopic);
        consumer.commitSync(); // commitSync venter på async funksjonen av å lage consumeren, så man vet consumeren er satt opp
        return consumer;
    }

    @SuppressWarnings("rawtypes")
    private <K,V> Consumer<K, V> buildConsumer(Class<? extends Deserializer> keyDeserializer,
                                               Class<? extends Deserializer> valueDeserializer) {
        // Use the procedure documented at https://docs.spring.io/spring-kafka/docs/2.2.4.RELEASE/reference/#embedded-kafka-annotation

        final Map<String, Object> consumerProps = KafkaTestUtils
                .consumerProps(UUID.randomUUID().toString(), "true", embeddedKafka);
        // Since we're pre-sending the messages to test for, we need to read from start of topic
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        // We need to match the ser/deser used in expected application config
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer.getName());
        consumerProps.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        consumerProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        final DefaultKafkaConsumerFactory<K, V> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        return consumerFactory.createConsumer();
    }

}
