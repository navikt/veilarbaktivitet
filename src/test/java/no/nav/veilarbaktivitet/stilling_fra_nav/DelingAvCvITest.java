package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avro.Arbeidssted;
import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.avro.SvarEnum;
import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.kvp.KvpClient;
import no.nav.veilarbaktivitet.oppfolging_status.OppfolgingStatusClient;
import no.nav.veilarbaktivitet.oppfolging_status.OppfolgingStatusDTO;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@EmbeddedKafka(topics = {"${topic.inn.stillingFraNav}","${topic.ut.stillingFraNav}"}, partitions = 1)
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

    private static final String AKTORID="aktorid";

    /***** Ekte bønner *****/

    @Autowired
    KafkaTemplate<String, ForesporselOmDelingAvCv> producer;

    @Autowired
    OpprettForesporselOmDelingAvCv service;

    /***** Mock bønner *****/

    @Autowired
    KvpClient kvpClient;

    @Autowired
    OppfolgingStatusClient oppfolgingStatusClient;

    /***** Bønner slutt *****/

    public Consumer<String, DelingAvCvRespons> createConsumer() {
        Consumer<String, DelingAvCvRespons> consumer = buildConsumer(
                StringDeserializer.class,
                KafkaAvroDeserializer.class
        );
        embeddedKafka.consumeFromEmbeddedTopics(consumer, utTopic);
        consumer.commitSync(); // commitSync venter på async funksjonen av å lage consumeren, så man vet consumeren er satt opp
        return consumer;
    }


    @After
    public void reset_mocks() {
        Mockito.reset(oppfolgingStatusClient, kvpClient);
    }

    @Test
    public void ikke_under_oppfolging() {
        final Consumer<String, DelingAvCvRespons> consumer = createConsumer();

        OppfolgingStatusDTO oppfolgingStatusDTO = OppfolgingStatusDTO.builder().underOppfolging(false).build();
        when(oppfolgingStatusClient.get(Person.aktorId(AKTORID))).thenReturn(Optional.of(oppfolgingStatusDTO));

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId);
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(AKTORID);
            assertions.assertThat(value.getAktivitetId()).isNull();
            assertions.assertThat(value.getBrukerVarslet()).isFalse();
            assertions.assertThat(value.getAktivitetOpprettet()).isFalse();
            assertions.assertThat(value.getBrukerSvar()).isEqualTo(SvarEnum.IKKE_SVART);
            assertions.assertAll();
        });

        verify(oppfolgingStatusClient).get(Person.aktorId(AKTORID));

    }

    @Test
    public void under_oppfolging_kvp() {
        final Consumer<String, DelingAvCvRespons> consumer = createConsumer();

        OppfolgingStatusDTO oppfolgingStatusDTO = OppfolgingStatusDTO.builder().underOppfolging(true).build();
        when(oppfolgingStatusClient.get(Person.aktorId(AKTORID))).thenReturn(Optional.of(oppfolgingStatusDTO));

        KvpDTO kvpDTO = new KvpDTO().setKvpId(999L).setAvsluttetDato(Date.from(Instant.now().minus(6, ChronoUnit.DAYS)));
        when(kvpClient.get(Person.aktorId(AKTORID))).thenReturn(Optional.of(kvpDTO));

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId);
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(AKTORID);
            assertions.assertThat(value.getAktivitetId()).isNull();
            assertions.assertThat(value.getBrukerVarslet()).isFalse();
            assertions.assertThat(value.getAktivitetOpprettet()).isFalse();
            assertions.assertThat(value.getBrukerSvar()).isEqualTo(SvarEnum.IKKE_SVART);
            assertions.assertAll();
        });

        verify(oppfolgingStatusClient).get(Person.aktorId(AKTORID));

    }

    @Test
    public void under_oppfolging_ikke_manuell_ikke_reservert_ikke_under_kvp() {
        final Consumer<String, DelingAvCvRespons> consumer = createConsumer();

        OppfolgingStatusDTO oppfolgingStatusDTO = OppfolgingStatusDTO.builder().underOppfolging(true).manuell(false).reservasjonKRR(false).build();
        when(oppfolgingStatusClient.get(Person.aktorId(AKTORID))).thenReturn(Optional.of(oppfolgingStatusDTO));
        KvpDTO kvpDTO = new KvpDTO().setKvpId(0L).setAvsluttetDato(null);
        when(kvpClient.get(Person.aktorId(AKTORID))).thenReturn(Optional.of(kvpDTO));

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId);
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(AKTORID);
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getBrukerVarslet()).isTrue();
            assertions.assertThat(value.getAktivitetOpprettet()).isTrue();
            assertions.assertThat(value.getBrukerSvar()).isEqualTo(SvarEnum.IKKE_SVART);
            assertions.assertAll();
        });

        verify(oppfolgingStatusClient).get(Person.aktorId(AKTORID));

    }

    @Test
    public void under_oppfolging_manuell_ikke_under_kvp() {
        final Consumer<String, DelingAvCvRespons> consumer = createConsumer();

        OppfolgingStatusDTO oppfolgingStatusDTO = OppfolgingStatusDTO.builder().underOppfolging(true).manuell(true).build();
        when(oppfolgingStatusClient.get(Person.aktorId(AKTORID))).thenReturn(Optional.of(oppfolgingStatusDTO));
        KvpDTO kvpDTO = new KvpDTO().setKvpId(0L).setAvsluttetDato(null);
        when(kvpClient.get(Person.aktorId(AKTORID))).thenReturn(Optional.of(kvpDTO));

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId);
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(AKTORID);
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getBrukerVarslet()).isFalse();
            assertions.assertThat(value.getAktivitetOpprettet()).isTrue();
            assertions.assertThat(value.getBrukerSvar()).isEqualTo(SvarEnum.IKKE_SVART);
            assertions.assertAll();
        });

        verify(oppfolgingStatusClient).get(Person.aktorId(AKTORID));

    }

    @Test
    public void under_oppfolging_ikke_manuell_reservert_i_krr_ikke_under_kvp() {
        final Consumer<String, DelingAvCvRespons> consumer = createConsumer();

        OppfolgingStatusDTO oppfolgingStatusDTO = OppfolgingStatusDTO.builder().underOppfolging(true).manuell(false).reservasjonKRR(true).build();
        when(oppfolgingStatusClient.get(Person.aktorId(AKTORID))).thenReturn(Optional.of(oppfolgingStatusDTO));
        KvpDTO kvpDTO = new KvpDTO().setKvpId(0L).setAvsluttetDato(null);
        when(kvpClient.get(Person.aktorId(AKTORID))).thenReturn(Optional.of(kvpDTO));

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId);
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(AKTORID);
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getBrukerVarslet()).isFalse();
            assertions.assertThat(value.getAktivitetOpprettet()).isTrue();
            assertions.assertThat(value.getBrukerSvar()).isEqualTo(SvarEnum.IKKE_SVART);
            assertions.assertAll();
        });

        verify(oppfolgingStatusClient).get(Person.aktorId(AKTORID));

    }

    @Test
    public void duplikat_bestillingsId_ignoreres() {
        final Consumer<String, DelingAvCvRespons> consumer = createConsumer();

        OppfolgingStatusDTO oppfolgingStatusDTO = OppfolgingStatusDTO.builder().underOppfolging(true).manuell(false).build();
        when(oppfolgingStatusClient.get(Person.aktorId(AKTORID))).thenReturn(Optional.of(oppfolgingStatusDTO));

        String bestillingsId = UUID.randomUUID().toString();
        ForesporselOmDelingAvCv melding = createMelding(bestillingsId);
        producer.send(innTopic, melding.getBestillingsId(), melding);


        final ConsumerRecord<String, DelingAvCvRespons> record = getSingleRecord(consumer, utTopic, 5000);
        DelingAvCvRespons value = record.value();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat(value.getBestillingsId()).isEqualTo(bestillingsId);
            assertions.assertThat(value.getAktorId()).isEqualTo(AKTORID);
            assertions.assertThat(value.getAktivitetId()).isNotEmpty();
            assertions.assertThat(value.getBrukerVarslet()).isTrue();
            assertions.assertThat(value.getAktivitetOpprettet()).isTrue();
            assertions.assertThat(value.getBrukerSvar()).isEqualTo(SvarEnum.IKKE_SVART);
            assertions.assertAll();
        });

        verify(oppfolgingStatusClient).get(Person.aktorId(AKTORID));

        ForesporselOmDelingAvCv duplikatMelding = createMelding(bestillingsId);
        producer.send(innTopic, duplikatMelding.getBestillingsId(), duplikatMelding);
        Exception exception = assertThrows(IllegalStateException.class, () -> getSingleRecord(consumer, utTopic, 5000));
        assertEquals("No records found for topic", exception.getMessage());
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

    static ForesporselOmDelingAvCv createMelding(String bestillingsId) {
        return ForesporselOmDelingAvCv.newBuilder()
                .setAktorId(AKTORID)
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


}
