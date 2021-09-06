package no.nav.veilarbaktivitet.config.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.utils.Credentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.stream.Collectors;

import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultConsumerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultProducerProperties;


@Configuration
@Profile("!dev") //TODO fiks denne
public class KafkaOnpremConfig {

    public static final String CONSUMER_GROUP_ID = "veilarbaktivitet-consumer";
    public static final String PRODUCER_CLIENT_ID = "veilarbaktivitet-producer";

    @Bean
    public KafkaConsumerClient consumerClient(
            List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> topicConfigs,
            Credentials credentials,
            KafkaOnpremProperties kafkaOnpremProperties,
            MeterRegistry meterRegistry
    ) {
        List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> config = topicConfigs
                .stream()
                .map(it -> it.withMetrics(meterRegistry))
                .map(KafkaConsumerClientBuilder.TopicConfig::withLogging)
                .collect(Collectors.toList());

        var client = KafkaConsumerClientBuilder.builder()
                .withProperties(onPremDefaultConsumerProperties(CONSUMER_GROUP_ID, kafkaOnpremProperties.getBrokersUrl(), credentials))
                .withTopicConfigs(config)
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
