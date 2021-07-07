package no.nav.veilarbaktivitet.config.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.utils.Credentials;
import no.nav.veilarbaktivitet.domain.kafka.OppfolgingAvsluttetKafkaDTO;
import no.nav.veilarbaktivitet.kvp.KvpAvsluttetKafkaDTO;
import no.nav.veilarbaktivitet.service.KafkaConsumerOnpremService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.jsonConsumer;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultConsumerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultProducerProperties;


@Configuration
@Profile("!dev") //TODO fiks denne
public class KafkaOnpremConfig {

    public static final String CONSUMER_GROUP_ID = "veilarbaktivitet-consumer";
    public static final String PRODUCER_CLIENT_ID = "veilarbaktivitet-producer";


    @Bean
    public Map<String, TopicConsumer<String, String>> topicConsumers(
            KafkaConsumerOnpremService kafkaConsumerOnpremService,
            KafkaOnpremProperties kafkaOnpremProperties
    ) {
        return Map.of(
                kafkaOnpremProperties.oppfolgingAvsluttetTopic,
                jsonConsumer(OppfolgingAvsluttetKafkaDTO.class, kafkaConsumerOnpremService::behandleOppfolgingAvsluttet),

                kafkaOnpremProperties.kvpAvsluttetTopic,
                jsonConsumer(KvpAvsluttetKafkaDTO.class, kafkaConsumerOnpremService::behandleKvpAvsluttet)
        );
    }

    @Bean
    public KafkaConsumerClient consumerClient(
            Map<String, TopicConsumer<String, String>> topicConsumers,
            Credentials credentials,
            KafkaOnpremProperties kafkaOnpremProperties,
            MeterRegistry meterRegistry
    ) {
        var client = KafkaConsumerClientBuilder.<String, String>builder()
                .withProperties(onPremDefaultConsumerProperties(CONSUMER_GROUP_ID, kafkaOnpremProperties.getBrokersUrl(), credentials))
                .withConsumers(topicConsumers)
                .withMetrics(meterRegistry)
                .withLogging()
                .build();

        client.start();

        return client;
    }

    @Bean
    public KafkaProducerClient<String, String> producerClient(KafkaOnpremProperties kafkaOnpremProperties, Credentials credentials, MeterRegistry meterRegistry) {
        return KafkaProducerClientBuilder.<String, String>builder()
                .withMetrics(meterRegistry)
                .withProperties(onPremDefaultProducerProperties(PRODUCER_CLIENT_ID, kafkaOnpremProperties.brokersUrl, credentials))
                .build();
    }

}
