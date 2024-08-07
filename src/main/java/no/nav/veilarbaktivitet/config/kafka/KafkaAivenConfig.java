package no.nav.veilarbaktivitet.config.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaAvroAvroTemplate;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringAvroTemplate;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
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
    <V extends SpecificRecordBase> ProducerFactory<String, V> stringAvroProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties(new DefaultSslBundleRegistry());
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    @Bean
    <K extends SpecificRecordBase, V extends SpecificRecordBase> ProducerFactory<K, V> avroAvroProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties(new DefaultSslBundleRegistry());
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    @Bean
    <V> ProducerFactory<String, V> jsonProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties(new DefaultSslBundleRegistry());
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    @Bean
    <V> ProducerFactory<String, V> stringProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties(new DefaultSslBundleRegistry());
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    @Bean
    <V> KafkaJsonTemplate<V> kafkaJsonTemplate(ProducerFactory<String, V> jsonProducerFactory) {
        return new KafkaJsonTemplate<>(jsonProducerFactory);
    }

    @Bean
    KafkaStringTemplate kafkaStringTemplate(ProducerFactory<String, String> stringProducerFactory) {
        return new KafkaStringTemplate(stringProducerFactory);
    }

    @Bean
    <V extends SpecificRecordBase> KafkaStringAvroTemplate<V> kafkaAvroTemplate(ProducerFactory<String, V> stringAvroProducerFactory) {
        return new KafkaStringAvroTemplate<>(stringAvroProducerFactory);
    }

    @Bean
    <K extends SpecificRecordBase, V extends SpecificRecordBase> KafkaAvroAvroTemplate<K, V> kafkaAvroAvroTemplate(ProducerFactory<K, V> avroAvroProducerFactory) {
        return new KafkaAvroAvroTemplate<>(avroAvroProducerFactory);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<SpecificRecordBase, SpecificRecordBase> avroAvrokafkaListenerContainerFactory(
            @Qualifier("avroAvroConsumerFactory")
            ConsumerFactory<SpecificRecordBase, SpecificRecordBase> avroAvroConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<SpecificRecordBase, SpecificRecordBase> factory = new ConcurrentKafkaListenerContainerFactory<>();
        return configureConcurrentKafkaListenerContainerFactory(avroAvroConsumerFactory, factory);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, SpecificRecordBase> stringAvroKafkaListenerContainerFactory(
            @Qualifier("stringAvroConsumerFactory")
            ConsumerFactory<String, SpecificRecordBase> stringAvroConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, SpecificRecordBase> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringAvroConsumerFactory);
        factory.getContainerProperties()
                .setAuthExceptionRetryInterval(Duration.ofSeconds(10L));
        factory.setConcurrency(3);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    @Bean
    DefaultErrorHandler errorHandler() {
        return new DefaultErrorHandler((rec, thr) -> log.error("Exception={} oppstått i kafka-consumer record til topic={}, partition={}, offset={}, bestillingsId={} feilmelding={}",
                thr.getClass().getSimpleName(),
                rec.topic(),
                rec.partition(),
                rec.offset(),
                rec.key(),
                thr.getCause()
        ),
                new FixedBackOff(DEFAULT_INTERVAL, UNLIMITED_ATTEMPTS));
    }

    <K extends SpecificRecordBase, V extends SpecificRecordBase> ConcurrentKafkaListenerContainerFactory<K, V> configureConcurrentKafkaListenerContainerFactory(
            @Qualifier("avroAvroConsumerFactory")
            ConsumerFactory<K, V> kafkaConsumerFactory, ConcurrentKafkaListenerContainerFactory<K, V> factory) {
        factory.setConsumerFactory(kafkaConsumerFactory);
        factory.getContainerProperties()
                .setAuthExceptionRetryInterval(Duration.ofSeconds(10L));

        factory.setConcurrency(3);
        factory.setCommonErrorHandler(errorHandler());

        return factory;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> stringStringKafkaListenerContainerFactory(
            @Qualifier("stringStringConsumerFactory")
            ConsumerFactory<String, String> stringStringConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringStringConsumerFactory);
        factory.getContainerProperties()
                .setAuthExceptionRetryInterval(Duration.ofSeconds(10L));

        factory.setConcurrency(3);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }


    @Bean(name = "stringStringConsumerFactory")
    ConsumerFactory<String, String> stringStringConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties(new DefaultSslBundleRegistry());
        consumerProperties.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProperties.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, org.apache.kafka.common.serialization.StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    @Bean(name = "stringAvroConsumerFactory")
    ConsumerFactory<String, SpecificRecordBase> stringAvroConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> consumerProperties =  kafkaProperties.buildConsumerProperties(new DefaultSslBundleRegistry());
        consumerProperties.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProperties.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    @Bean(name = "avroAvroConsumerFactory")
    ConsumerFactory<SpecificRecordBase, SpecificRecordBase> avroAvroConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties(new DefaultSslBundleRegistry());
        consumerProperties.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        consumerProperties.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

}
