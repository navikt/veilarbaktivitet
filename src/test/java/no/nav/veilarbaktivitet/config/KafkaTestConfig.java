package no.nav.veilarbaktivitet.config;

import no.nav.common.kafka.util.KafkaPropertiesBuilder;
import no.nav.veilarbaktivitet.config.kafka.KafkaOnpremProperties;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.core.BrokerAddress;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Configuration
public class KafkaTestConfig {
    @Bean
    public EmbeddedKafkaBroker embeddedKafka(
            @Value("${topic.inn.stillingFraNav}") String innStillingFraNav,
            @Value("${topic.ut.stillingFraNav}") String utStillingFraNav,
            @Value("${app.kafka.endringPaaAktivitetTopic}") String endringPaaAktivitetTopic,
            @Value("${app.kafka.oppfolgingAvsluttetTopic}") String oppfolgingAvsluttetTopic,
            @Value("${app.kafka.kvpAvsluttetTopic}") String kvpAvsluttetTopic,
            @Value("${topic.inn.eksternVarselKvittering}") String ekstertVarselKvitering,
            @Value("${topic.ut.aktivitetdata.rawjson}") String aktivitetRawJson,
            @Value("${topic.ut.portefolje}") String portefoljeTopic) {
        // TODO config
        return new EmbeddedKafkaBroker(
                1,
                true,
                1,
                innStillingFraNav,
                utStillingFraNav,
                endringPaaAktivitetTopic,
                oppfolgingAvsluttetTopic,
                kvpAvsluttetTopic,
                ekstertVarselKvitering,
                aktivitetRawJson,
                portefoljeTopic);
    }

    @Bean
    KafkaTemplate<String, String> kafkaStringTemplate(ProducerFactory<String, String> stringProducerFactory) {
        return new KafkaTemplate<>(stringProducerFactory);
    }

    @Bean
    ProducerFactory<String, String> stringProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }


    @Bean
    @Primary
    KafkaProperties kafkaProperties(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafkaBroker) {
        kafkaProperties.setBootstrapServers(Arrays.stream(embeddedKafkaBroker.getBrokerAddresses()).map(BrokerAddress::toString).collect(Collectors.toList()));
        return kafkaProperties;
    }

    @Bean
    @Primary
    <V extends SpecificRecordBase> KafkaTemplate<String, V> kafkaAvroTestTemplate(KafkaTemplate<String, V> kafkaAvroTemplate) {
        return Mockito.spy(kafkaAvroTemplate);
    }

    @Bean
    <V extends SpecificRecordBase> ConsumerFactory<String, V> stringAvroConsumerFactory(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafka) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    @Bean
    <K extends SpecificRecordBase, V extends SpecificRecordBase> ConsumerFactory<K, V> avroAvroConsumerFactory(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafka) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    @Bean
    <V> ConsumerFactory<String, V> stringJsonConsumerFactory(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafka) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonDeserializer.class);
        consumerProperties.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }


    @Bean
    ConsumerFactory<String, String> stringStringConsumerFactory(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafka) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    @Bean
    Properties onPremProducerProperties(KafkaOnpremProperties kafkaOnpremProperties, EmbeddedKafkaBroker embeddedKafka) {
        return KafkaPropertiesBuilder.producerBuilder()
                .withBaseProperties()
                .withProducerId(kafkaOnpremProperties.getProducerClientId())
                .withBrokerUrl(embeddedKafka.getBrokersAsString())
                .withSerializers(StringSerializer.class, StringSerializer.class)
                .build();
    }

    @Bean
    public Admin kafkaAdminClient(KafkaProperties properties, EmbeddedKafkaBroker embeddedKafkaBroker) {
        Map<String, Object> config = properties.buildAdminProperties();
        config.put("bootstrap.servers", embeddedKafkaBroker.getBrokersAsString());
        return Admin.create(config);
    }

    @Bean
    Properties onPremConsumerProperties(KafkaOnpremProperties kafkaOnpremProperties, EmbeddedKafkaBroker embeddedKafka) {
        return KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties()
                .withConsumerGroupId(kafkaOnpremProperties.getConsumerGroupId())
                .withBrokerUrl(embeddedKafka.getBrokersAsString())
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .withPollProperties(1, 1000)
                .build();
    }
}
