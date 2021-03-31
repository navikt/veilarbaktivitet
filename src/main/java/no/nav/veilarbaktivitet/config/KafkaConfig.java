package no.nav.veilarbaktivitet.config;

import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.core.LockProvider;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.OracleConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.StoredRecordConsumer;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRepository;
import no.nav.common.kafka.producer.feilhandtering.OracleProducerRepository;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.utils.Credentials;
import no.nav.veilarbaktivitet.domain.kafka.KvpAvsluttetKafkaDTO;
import no.nav.veilarbaktivitet.domain.kafka.OppfolgingAvsluttetKafkaDTO;
import no.nav.veilarbaktivitet.service.KafkaConsumerService;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Map;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.jsonConsumer;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultConsumerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultProducerProperties;

@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaConfig {

    public final static String CONSUMER_GROUP_ID = "veilarbaktivitet-consumer";
    public final static String PRODUCER_CLIENT_ID = "veilarbaktivitet-producer";

    @Autowired
    KafkaConsumerClient<String, String> consumerClient;

    @Autowired
    KafkaConsumerRecordProcessor consumerRecordProcessor;

    @Bean
    public KafkaConsumerRepository kafkaConsumerRepository(DataSource dataSource) {
        return new OracleConsumerRepository(dataSource);
    }

    @Bean
    public KafkaProducerRepository producerRepository(DataSource dataSource) {
        return new OracleProducerRepository(dataSource);
    }

    @Bean
    public Map<String, TopicConsumer<String, String>> topicConsumers(
            KafkaConsumerService kafkaConsumerService,
            KafkaProperties kafkaProperties
    ) {
        return Map.of(
                kafkaProperties.oppfolgingAvsluttetTopic,
                jsonConsumer(OppfolgingAvsluttetKafkaDTO.class, kafkaConsumerService::behandleOppfolgingAvsluttet),

                kafkaProperties.kvpAvsluttetTopic,
                jsonConsumer(KvpAvsluttetKafkaDTO.class, kafkaConsumerService::behandleKvpAvsluttet)
        );
    }

    @Bean
    public KafkaConsumerClient<String, String> consumerClient(
            Map<String, TopicConsumer<String, String>> topicConsumers,
            KafkaConsumerRepository kafkaConsumerRepository,
            Credentials credentials,
            KafkaProperties kafkaProperties,
            MeterRegistry meterRegistry
    ) {
        return KafkaConsumerClientBuilder.<String, String>builder()
                .withProps(onPremDefaultConsumerProperties(CONSUMER_GROUP_ID, kafkaProperties.getBrokersUrl(), credentials))
                .withRepository(kafkaConsumerRepository)
                .withSerializers(new StringSerializer(), new StringSerializer())
                .withStoreOnFailureConsumers(topicConsumers)
                .withMetrics(meterRegistry)
                .withLogging()
                .build();
    }


    @Bean
    public KafkaConsumerRecordProcessor consumerRecordProcessor(
            LockProvider lockProvider,
            KafkaConsumerRepository kafkaConsumerRepository,
            Map<String, TopicConsumer<String, String>> topicConsumers
    ) {
        Map<String, StoredRecordConsumer> storedRecordConsumers = ConsumerUtils.toStoredRecordConsumerMap(
                topicConsumers,
                new StringDeserializer(),
                new StringDeserializer()
        );

        return new KafkaConsumerRecordProcessor(lockProvider, kafkaConsumerRepository, storedRecordConsumers);
    }

    @Bean
    public KafkaProducerClient<String, String> producerClient(KafkaProperties kafkaProperties, Credentials credentials, MeterRegistry meterRegistry) {
        return KafkaProducerClientBuilder.<String, String>builder()
                .withMetrics(meterRegistry)
                .withProperties(onPremDefaultProducerProperties(PRODUCER_CLIENT_ID, kafkaProperties.brokersUrl, credentials))
                .build();
    }

    @PostConstruct
    public void start() {
        consumerClient.start();
        consumerRecordProcessor.start();
    }

}
