package no.nav.veilarbaktivitet.config.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder.TopicConfig;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Properties;

import static no.nav.common.kafka.util.KafkaPropertiesPreset.aivenDefaultConsumerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.aivenDefaultProducerProperties;


@Configuration
public class NavCommonKafkaConfig {

    public static final String CONSUMER_GROUP_ID = "veilarbaktivitet-consumer-aiven";
    public static final String PRODUCER_CLIENT_ID = "veilarbaktivitet-producer";
    private static final String AIVEN_KAFKA_DISABLED = "veilarbaktivitet.kafka.aiven.consumer.disabled";

    @Bean
    public KafkaConsumerClient aivenConsumerClient(
            List<AivenConsumerConfig<?, ?>> topicConfigs,
            MeterRegistry meterRegistry,
            Properties aivenConsumerProperties,
            UnleashClient unleashClient
    ) {
        var clientBuilder = KafkaConsumerClientBuilder.builder()
                .withProperties(aivenConsumerProperties)
                .withToggle(() -> unleashClient.isEnabled(AIVEN_KAFKA_DISABLED));

        topicConfigs.forEach(it -> clientBuilder.withTopicConfig(new TopicConfig().withConsumerConfig(it).withMetrics(meterRegistry).withLogging()));

        var client = clientBuilder.build();

        client.start();

        return client;
    }

    @Bean
    public KafkaProducerClient<String, String> aivenProducerClient(Properties aivenProducerProperties, MeterRegistry meterRegistry) {
        return KafkaProducerClientBuilder.<String, String>builder()
                .withMetrics(meterRegistry)
                .withProperties(aivenProducerProperties)
                .build();
    }

    @Bean
    @Profile("!dev")
    Properties aivenConsumerProperties() {
        return aivenDefaultConsumerProperties(CONSUMER_GROUP_ID);
    }

    @Bean
    @Profile("!dev")
    Properties aivenProducerProperties() {
        return aivenDefaultProducerProperties(PRODUCER_CLIENT_ID);
    }
}
