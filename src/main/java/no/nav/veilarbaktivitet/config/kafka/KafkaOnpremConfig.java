package no.nav.veilarbaktivitet.config.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder.TopicConfig;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.utils.Credentials;
import no.nav.doknotifikasjon.schemas.DoknotifikasjonStatus;
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
@Slf4j
public class KafkaOnpremConfig {

    //@Bean
    public KafkaConsumerClient consumerClient(
            List<TopicConsumerConfig<?, ?>> topicConfigs,
            MeterRegistry meterRegistry,
            Properties onPremConsumerProperties
    ) {
        var clientBuilder = KafkaConsumerClientBuilder.builder()
                .withProperties(onPremConsumerProperties);

        topicConfigs.forEach(it -> {
            clientBuilder.withTopicConfig(new TopicConfig().withConsumerConfig(it).withMetrics(meterRegistry).withLogging());
        });

        var client = clientBuilder.build();

        client.start();

        return client;
    }

    @Bean
    public KafkaConsumerClient consumerClient2(
            TopicConsumerConfig<String, DoknotifikasjonStatus> topicConfigs,
            MeterRegistry meterRegistry,
            Properties onPremConsumerProperties
    ) {
        var clientBuilder = KafkaConsumerClientBuilder.builder()
                .withProperties(onPremConsumerProperties);


        log.info("stuff", onPremConsumerProperties);
        clientBuilder.withTopicConfig(new TopicConfig().withConsumerConfig(topicConfigs).withMetrics(meterRegistry).withLogging());


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
        return onPremDefaultProducerProperties(kafkaOnpremProperties.producerClientId + "3kake", kafkaOnpremProperties.brokersUrl, credentials);
    }

    @Bean
    @Profile("!dev")
    Properties onPremConsumerProperties(KafkaOnpremProperties kafkaOnpremProperties, Credentials credentials) {
        return onPremDefaultConsumerProperties(kafkaOnpremProperties.consumerGroupId + "2kake", kafkaOnpremProperties.getBrokersUrl(), credentials);
    }

}
