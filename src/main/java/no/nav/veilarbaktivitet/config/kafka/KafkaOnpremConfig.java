package no.nav.veilarbaktivitet.config.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder.TopicConfig;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.utils.Credentials;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultConsumerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultProducerProperties;


@Configuration
public class KafkaOnpremConfig {

    private static final String ONPREM_KAFKA_DISABLED = "veilarbaktivitet.kafka.onprem.consumer.disabled";

    @Bean
    public KafkaConsumerClient consumerClient(
            List<TopicConsumerConfig<?, ?>> topicConfigs,
            MeterRegistry meterRegistry,
            Properties onPremConsumerProperties,
            UnleashClient unleashClient
    ) {
        var clientBuilder = KafkaConsumerClientBuilder.builder()
                .withProperties(onPremConsumerProperties)
                .withToggle(() -> unleashClient.isEnabled(ONPREM_KAFKA_DISABLED));

        topicConfigs.forEach(it -> {
            clientBuilder.withTopicConfig(new TopicConfig().withConsumerConfig(it).withMetrics(meterRegistry).withLogging());
        });

        var client = clientBuilder.build();

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
    <V extends SpecificRecordBase> Deserializer<V> onpremSchemaRegistryUrl(
            @Value("${app.kafka.schema-regestry-url}")
                    String onpremSchemaRegistryUrl
    ) {
        HashMap<String, Object> props = new HashMap<>();
        props.put(SPECIFIC_AVRO_READER_CONFIG, true);
        props.put("schema.registry.url", onpremSchemaRegistryUrl);
        return Deserializers.onPremAvroDeserializer(onpremSchemaRegistryUrl, props);
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
