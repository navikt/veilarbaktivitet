package no.nav.veilarbaktivitet.config.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static no.nav.common.kafka.util.KafkaPropertiesPreset.*;

@Configuration
public class KafkaAivenConfig {

    public static final String CONSUMER_GROUP_ID = "veilarbaktivitet-consumer";
    public static final String PRODUCER_CLIENT_ID = "veilarbaktivitet-producer";


// TODO @Bean
    public KafkaConsumerClient consumerClient(
            MeterRegistry meterRegistry,
            JsonConsumerWrapper... consumers
    ) {

        Map<String, TopicConsumer<String, String>> consumerMap = Arrays.stream(consumers).collect(Collectors.toMap(JsonConsumerWrapper::getTopic, JsonConsumerWrapper::getConsumer));

        var client = KafkaConsumerClientBuilder.<String, String>builder()
                .withProperties(aivenDefaultConsumerProperties(CONSUMER_GROUP_ID))
                .withConsumers(consumerMap)
                .withMetrics(meterRegistry)
                .withLogging()
                .build();

        client.start();

        return client;
    }

    @Bean
    public KafkaProducerClient<String, String> aivenProducerClient(MeterRegistry meterRegistry) {
        return KafkaProducerClientBuilder.<String, String>builder()
                .withMetrics(meterRegistry)
                .withProperties(aivenDefaultProducerProperties(PRODUCER_CLIENT_ID))
                .build();
    }


}
