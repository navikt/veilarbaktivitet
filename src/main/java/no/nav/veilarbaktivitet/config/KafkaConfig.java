package no.nav.veilarbaktivitet.config;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.utils.Credentials;
import no.nav.veilarbaktivitet.domain.kafka.OppfolgingAvsluttetKafkaDTO;
import no.nav.veilarbaktivitet.kvp.KvpAvsluttetKafkaDTO;
import no.nav.veilarbaktivitet.service.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.jsonConsumer;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultConsumerProperties;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.onPremDefaultProducerProperties;

@Configuration
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaConfig {

    public static final String CONSUMER_GROUP_ID = "veilarbaktivitet-consumer";
    public static final String PRODUCER_CLIENT_ID = "veilarbaktivitet-producer";

    @Autowired
    KafkaConsumerClient<String, String> consumerClient;

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
            Credentials credentials,
            KafkaProperties kafkaProperties,
            MeterRegistry meterRegistry
    ) {
        return KafkaConsumerClientBuilder.<String, String>builder()
                .withProps(onPremDefaultConsumerProperties(CONSUMER_GROUP_ID, kafkaProperties.getBrokersUrl(), credentials))
                .withConsumers(topicConsumers)
                .withMetrics(meterRegistry)
                .withLogging()
                .build();
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
    }

}
