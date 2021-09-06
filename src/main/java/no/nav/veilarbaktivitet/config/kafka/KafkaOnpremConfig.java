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
import java.util.Properties;
import java.util.stream.Collectors;

import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultConsumerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultProducerProperties;


@Configuration
public class KafkaOnpremConfig {

    public static final String CONSUMER_GROUP_ID = "veilarbaktivitet-consumer";
    public static final String PRODUCER_CLIENT_ID = "veilarbaktivitet-producer";

    @Bean
    public KafkaConsumerClient consumerClient(
            List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> topicConfigs,
            Properties onPremConsumerProperties,
            MeterRegistry meterRegistry
    ) {
        List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> config = topicConfigs
                .stream()
                .map(it -> it.withMetrics(meterRegistry))
                .map(KafkaConsumerClientBuilder.TopicConfig::withLogging)
                .collect(Collectors.toList());

        var client = KafkaConsumerClientBuilder.builder()
                .withProperties(onPremConsumerProperties)
                .withTopicConfigs(config)
                .build();

        client.start();

        return client;
    }

    @Bean
    public KafkaProducerClient<String, String> producerClient(Properties onPremProducerProperties, MeterRegistry meterRegistry) {
        return KafkaProducerClientBuilder.<String, String>builder()
                .withMetrics(meterRegistry)
                .withProperties(onPremProducerProperties)
                .build();
    }

    @Bean
    @Profile("!dev")
    Properties onPremProducerProperties(KafkaOnpremProperties kafkaOnpremProperties, Credentials credentials) {
        return onPremDefaultProducerProperties(kafkaOnpremProperties.producerClientId, kafkaOnpremProperties.brokersUrl, credentials);
    }

    @Bean
    @Profile("!dev")
    Properties onPremConsumerProperties(KafkaOnpremProperties kafkaOnpremProperties, Credentials credentials) {
        return onPremDefaultConsumerProperties(kafkaOnpremProperties.consumerGroupId, kafkaOnpremProperties.getBrokersUrl(), credentials);
    }

}
