package no.nav.veilarbaktivitet.config.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder.TopicConfig;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetsKortConsumerConfig;
import no.nav.veilarbaktivitet.aktivitetskort.test.AktivitetsKortTestConsumerConfig;
import no.nav.veilarbaktivitet.kvp.KvpAvsluttetConsumerConfig;
import no.nav.veilarbaktivitet.kvp.KvpAvsluttetKafkaDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Properties;

import static no.nav.common.kafka.util.KafkaPropertiesPreset.aivenDefaultConsumerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.aivenDefaultProducerProperties;


@Configuration
public class NavCommonKafkaConfig {

    public static final String CONSUMER_GROUP_ID = "veilarbaktivitet-consumer-aiven";
    public static final String PRODUCER_CLIENT_ID = "veilarbaktivitet-producer";
    private static final String AKTIVITETSKORT_KAFKACONSUMER_DISABLED = "veilarbaktivitet.kafka.aktivitetskort.aiven.consumer.disabled";
    private static final String KVPAVSLUTTET_KAFKACONSUMER_DISABLED = "veilarbaktivitet.kafka.kvpavsluttet.aiven.consumer.disabled";

    @Bean
    public KafkaConsumerClient aktivitetskortTestConsumerClient(
            AktivitetsKortTestConsumerConfig topicConfig,
            MeterRegistry meterRegistry,
            Properties testAivenConsumerProperties,
            UnleashClient unleashClient
    ) {
        var clientBuilder = KafkaConsumerClientBuilder.builder()
                .withProperties(testAivenConsumerProperties)
                .withToggle(() -> unleashClient.isEnabled("veilarbaktivitet.kafka.aktivitetskorttest.aiven.consumer.disabled"))
                .withTopicConfig(
                        new TopicConfig<String, String>()
                                .withConsumerConfig(topicConfig)
                                .withMetrics(meterRegistry)
                                .withLogging());

        var client = clientBuilder.build();

        client.start();

        return client;
    }

    @Bean
    public KafkaConsumerClient aktivitetskortConsumerClient(
            AktivitetsKortConsumerConfig topicConfig,
            MeterRegistry meterRegistry,
            Properties aivenConsumerProperties,
            UnleashClient unleashClient
    ) {
        var clientBuilder = KafkaConsumerClientBuilder.builder()
                .withProperties(aivenConsumerProperties)
                .withToggle(() -> unleashClient.isEnabled(AKTIVITETSKORT_KAFKACONSUMER_DISABLED))
                .withTopicConfig(
                        new TopicConfig<String, String>()
                                .withConsumerConfig(topicConfig)
                                .withMetrics(meterRegistry)
                                .withLogging());

        var client = clientBuilder.build();

        client.start();

        return client;
    }

    @Bean
    public KafkaConsumerClient kvpAvsluttetConsumerClient(
            KvpAvsluttetConsumerConfig topicConfig,
            MeterRegistry meterRegistry,
            Properties aivenConsumerProperties,
            UnleashClient unleashClient
    ) {
        var clientBuilder = KafkaConsumerClientBuilder.builder()
                .withProperties(aivenConsumerProperties)
                .withToggle(() -> unleashClient.isEnabled(KVPAVSLUTTET_KAFKACONSUMER_DISABLED))
                .withTopicConfig(
                        new TopicConfig<String, KvpAvsluttetKafkaDTO>()
                                .withConsumerConfig(topicConfig)
                                .withMetrics(meterRegistry)
                                .withLogging());

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
    Properties testAivenConsumerProperties() {
        return aivenDefaultConsumerProperties("veilarbaktivitet-test-consumer-aiven-2");
    }

    @Bean
    @Profile("!dev")
    Properties aivenProducerProperties() {
        return aivenDefaultProducerProperties(PRODUCER_CLIENT_ID);
    }
}
