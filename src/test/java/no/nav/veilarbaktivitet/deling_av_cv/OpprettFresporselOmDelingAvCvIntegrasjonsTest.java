package no.nav.veilarbaktivitet.deling_av_cv;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.kafka.util.KafkaEnvironmentVariables;
import no.nav.common.kafka.util.KafkaPropertiesBuilder;
import no.nav.common.utils.AssertUtils;
import no.nav.veilarbaktivitet.avro.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.config.kafka.AvroTopicConsumer;
import no.nav.veilarbaktivitet.config.kafka.KafkaAivenConfig;
import no.nav.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.veilarbaktivitet.kvp.KvpService;
import no.nav.veilarbaktivitet.oppfolging_status.OppfolgingStatusClient;
import no.nav.veilarbaktivitet.service.AktivitetService;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.Properties;

@RunWith(SpringRunner.class)
@Slf4j
public class OpprettFresporselOmDelingAvCvIntegrasjonsTest {
    public static final String INN_TOPIC = "deling-av-stilling-fra-nav-forespurt-v1";
    public static final String UT_TOPIC = "stilling-fra-nav-oppdatert-v1";
    public static final String KAFKA_IMAGE = "confluentinc/cp-kafka:5.4.3";
    @ClassRule
    public static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));

    @TestConfiguration
    static class Config {

        @Bean
        public KafkaProducerClient<String, GenericRecord> aivenAvroProducerClient(MeterRegistry meterRegistry) {
            return KafkaProducerClientBuilder.<String, GenericRecord>builder()
                    .withMetrics(meterRegistry)
                    .withProperties(aivenAvroProducerProperties(KafkaAivenConfig.PRODUCER_CLIENT_ID))
                    .build();
        }


        public static Properties aivenAvroProducerProperties(String producerId) {
            return KafkaPropertiesBuilder.producerBuilder()
                    .withBaseProperties()
                    .withProducerId(producerId)
                    .withAivenBrokerUrl()
                    //   .withAivenAuth()
                    .withSerializers(StringSerializer.class, KafkaAvroSerializer.class)
                    .withProp(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://test")
                    .build();
        }

        @Bean
        public AvroTopicConsumer<String, ForesporselOmDelingAvCv> avroTopicConsumer() {
            return new AvroTopicConsumer<>((foresporselOmDelingAvCv) -> {
                log.info("Consumer leser: {}", foresporselOmDelingAvCv);
                return null;
            });
        }

        @Bean
        public KafkaConsumerClient aivenAvroConsumerClient(MeterRegistry meterRegistry, AvroTopicConsumer avroTopicConsumer) {
            return KafkaConsumerClientBuilder.<String, ForesporselOmDelingAvCv>builder()
                    .withMetrics(meterRegistry)
                    .withProperties(aivenAvroConsumerProperties("groupId"))
                    .withConsumer(avroTopicConsumer.getTopic(), avroTopicConsumer)
                    .build();
        }

        public static Properties aivenAvroConsumerProperties(String groupId) {
            return KafkaPropertiesBuilder.consumerBuilder()
                    .withBaseProperties()
                    .withConsumerGroupId(groupId)
                    .withDeserializers(StringDeserializer.class, KafkaAvroDeserializer.class)
                    .withProp(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "mock://test")
                    .withAivenBrokerUrl()
                    .build();
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

    }

    @Autowired
    KafkaProducerClient<String, GenericRecord> aivenAvroProducerClient;
    @Autowired
    KafkaConsumerClient aivenAvroConsumerClient;

    @MockBean
    KvpService kvpService;
    @MockBean
    OppfolgingStatusClient oppfolgingStatusClient;
    @MockBean
    public AktivitetService aktivitetService;
    @MockBean
    public DelingAvCvService delingAvCvService;
    @MockBean
    public AktivitetDAO aktivitetDAO;
    @MockBean
    public DelingAvCvDAO delingAvCvDAO;


    @BeforeClass
    public static void setup() {
        String brokerUrl = kafka.getBootstrapServers();


        System.setProperty(KafkaEnvironmentVariables.KAFKA_BROKERS, brokerUrl);

        AdminClient admin = KafkaAdminClient.create(Map.of(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokerUrl));
        admin.deleteTopics(List.of(INN_TOPIC, UT_TOPIC));
        admin.createTopics(List.of(
                new NewTopic(INN_TOPIC, 1, (short) 1),
                new NewTopic(UT_TOPIC, 1, (short) 1)
                )
        );
        admin.close(); // Apply changes
    }

    @Test
    public void kanari() {
    }


    @Test
    public void testProducer() {
        ForesporselOmDelingAvCv melding = OpprettForesporselOmDelingAvCvTest.createMelding();
        RecordMetadata recordMetadata = aivenAvroProducerClient.sendSync(new ProducerRecord<>(INN_TOPIC, melding));
        AssertUtils.assertTrue(recordMetadata.hasOffset());
    }

    @Test
    public void testConsumer() {
        ForesporselOmDelingAvCv melding = OpprettForesporselOmDelingAvCvTest.createMelding();
        RecordMetadata recordMetadata = aivenAvroProducerClient.sendSync(new ProducerRecord<>(INN_TOPIC, melding));
        AssertUtils.assertTrue(recordMetadata.hasOffset());
        aivenAvroConsumerClient.start();
        // TODO not working

    }
}
