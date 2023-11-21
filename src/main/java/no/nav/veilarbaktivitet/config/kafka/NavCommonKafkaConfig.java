package no.nav.veilarbaktivitet.config.kafka;

import io.getunleash.Unleash;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder.TopicConfig;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetsKortConsumerConfig;
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
    public static final String AKTIVITETSKORT_CONSUMER_GROUP = "veilarbaktivitet-aktivitetskort-consumer";
    public static final String PRODUCER_CLIENT_ID = "veilarbaktivitet-producer";
    private static final String AKTIVITETSKORT_KAFKACONSUMER_DISABLED = "veilarbaktivitet.kafka.aktivitetskort.aiven.consumer.disabled";
    private static final String KVPAVSLUTTET_KAFKACONSUMER_DISABLED = "veilarbaktivitet.kafka.kvpavsluttet.aiven.consumer.disabled";


    @Bean
    public KafkaConsumerClient aktivitetskortConsumerClient(
            AktivitetsKortConsumerConfig topicConfig,
            MeterRegistry meterRegistry,
            Properties aktivitetskortConsumerProperties,
            Unleash unleash
    ) {
        var clientBuilder = KafkaConsumerClientBuilder.builder()
                .withProperties(aktivitetskortConsumerProperties)
                .withToggle(() -> unleash.isEnabled(AKTIVITETSKORT_KAFKACONSUMER_DISABLED))
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
    public KafkaConsumerClient oppfolgingsperiodeConsumerClient(
            AktivitetsKortConsumerConfig topicConfig,
            MeterRegistry meterRegistry,
            Properties aktivitetskortConsumerProperties,
            Unleash unleash
    ) {
        var clientBuilder = KafkaConsumerClientBuilder.builder()
                .withProperties(aktivitetskortConsumerProperties)
                .withToggle(() -> unleash.isEnabled(AKTIVITETSKORT_KAFKACONSUMER_DISABLED))
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
            Properties kvpAvsluttetConsumerProperties,
            Unleash unleash
    ) {
        var clientBuilder = KafkaConsumerClientBuilder.builder()
                .withProperties(kvpAvsluttetConsumerProperties)
                .withToggle(() -> unleash.isEnabled(KVPAVSLUTTET_KAFKACONSUMER_DISABLED))
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
    Properties aktivitetskortConsumerProperties() {
        return aivenDefaultConsumerProperties(AKTIVITETSKORT_CONSUMER_GROUP);
    }

    @Bean
    @Profile("!dev")
    Properties kvpAvsluttetConsumerProperties() {
        return aivenDefaultConsumerProperties(CONSUMER_GROUP_ID);
    }


    @Bean
    @Profile("!dev")
    Properties aivenProducerProperties() {
        return aivenDefaultProducerProperties(PRODUCER_CLIENT_ID);
    }
}
