package no.nav.veilarbaktivitet.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;
import java.util.Map;

import static org.springframework.util.backoff.FixedBackOff.DEFAULT_INTERVAL;
import static org.springframework.util.backoff.FixedBackOff.UNLIMITED_ATTEMPTS;

@Slf4j
@EnableKafka
@Configuration
public class KafkaAivenConfig {

    @Bean
    void kafkaListenerContainerFactory() {
        // org.springframework.boot.autoconfigure.kafka.KafkaAnnotationDrivenConfiguration.kafkaListenerContainerFactory
        // For aa override spring default config
    }

    @Bean
    <V extends SpecificRecordBase> ProducerFactory<String, V> avroProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    @Bean
    <V extends SpecificRecordBase> ProducerFactory<String, V> jsonProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    @Bean
    <V> KafkaJsonTemplate<String, V> kafkaJsonTemplate(ProducerFactory<String, V> jsonProducerFactory) {
        return new KafkaJsonTemplate<>(jsonProducerFactory);
    }

    @Bean
    <V extends SpecificRecordBase> KafkaTemplate<String, V> kafkaAvroTemplate(ProducerFactory<String, V> avroProducerFactory) {
        return new KafkaTemplate<>(avroProducerFactory);
    }

    @Bean
    <K extends SpecificRecordBase, V extends SpecificRecordBase> ConcurrentKafkaListenerContainerFactory<K, V> avroAvrokafkaListenerContainerFactory(
            ConsumerFactory<K, V> kafkaConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<K, V> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory);
        factory.getContainerProperties()
                .setAuthorizationExceptionRetryInterval(Duration.ofSeconds(10L));

        factory.setConcurrency(3);
        factory.setErrorHandler(new SeekToCurrentErrorHandler(
                (rec, thr) -> log.error("Exception={} oppstått i kafka-consumer record til topic={}, partition={}, offset={}, bestillingsId={} feilmelding={}",
                        thr.getClass().getSimpleName(),
                        rec.topic(),
                        rec.partition(),
                        rec.offset(),
                        rec.key(),
                        thr.getCause()
                ),
                new FixedBackOff(DEFAULT_INTERVAL, UNLIMITED_ATTEMPTS)));
        return factory;
    }

    @Bean
    <V extends SpecificRecordBase> ConcurrentKafkaListenerContainerFactory<String, V> stringAvroKafkaListenerContainerFactory(
            ConsumerFactory<String, V> kafkaConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, V> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory);
        factory.getContainerProperties()
                .setAuthorizationExceptionRetryInterval(Duration.ofSeconds(10L));

        factory.setConcurrency(3);
        factory.setErrorHandler(new SeekToCurrentErrorHandler(
                (rec, thr) -> log.error("Exception={} oppstått i kafka-consumer record til topic={}, partition={}, offset={}, bestillingsId={} feilmelding={}",
                        thr.getClass().getSimpleName(),
                        rec.topic(),
                        rec.partition(),
                        rec.offset(),
                        rec.key(),
                        thr.getCause()
                ),
                new FixedBackOff(DEFAULT_INTERVAL, UNLIMITED_ATTEMPTS)));
        return factory;
    }
}
