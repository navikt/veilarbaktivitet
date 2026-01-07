package no.nav.veilarbaktivitet.config;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.kafka.producer.util.KafkaProducerClientBuilder;
import no.nav.common.kafka.util.KafkaPropertiesBuilder;
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate;
import no.nav.veilarbaktivitet.util.NavCommonKafkaSerialized;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

@Configuration
public class KafkaTestConfig {
    @Bean
    public EmbeddedKafkaKraftBroker embeddedKafka(
            @Value("${topic.inn.stillingFraNav}") String innStillingFraNav,
            @Value("${topic.ut.stillingFraNav}") String utStillingFraNav,
            @Value("${topic.inn.kvpAvsluttet}") String kvpAvsluttetTopic,
//            @Value("${topic.inn.eksternVarselKvittering}") String eksternVarselKvittering,
            @Value("${topic.ut.portefolje}") String portefoljeTopic,
            @Value("${topic.inn.oppfolgingsperiode}") String oppfolgingsperiode,
            @Value("${topic.inn.aktivitetskort}") String aktivitetskortTopic) {
        // TODO config
        return new EmbeddedKafkaKraftBroker(
                1,
                1,
                innStillingFraNav,
                utStillingFraNav,
                kvpAvsluttetTopic,
//                eksternVarselKvittering,
                portefoljeTopic,
                oppfolgingsperiode,
                aktivitetskortTopic);
    }

    @Bean
    @Primary
    KafkaProperties kafkaProperties(KafkaProperties kafkaProperties, EmbeddedKafkaKraftBroker embeddedKafkaBroker) {
        kafkaProperties.setBootstrapServers(Arrays.stream(embeddedKafkaBroker.getBrokersAsString().split(",")).toList());
        return kafkaProperties;
    }

    @Qualifier("stringJsonConsumerFactory")
    @Bean
    <V> ConsumerFactory<String, V> stringJsonConsumerFactory(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafka) {
        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(kafkaProperties.getConsumer().getGroupId(), "true", embeddedKafka);
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonDeserializer.class);
        consumerProperties.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

 // Denne er opprettet spesifikt for å støtte JsonSerialiseren fra nav.common.kafka
    @Bean
    <V> ProducerFactory<String, V> navCommonJsonProducerFactory(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafkaBroker) {
        Map<String, Object> producerProperties = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, no.nav.common.kafka.producer.serializer.JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    /*
    Kafka template som bruker nav.common.kafka sin json serializer.
    Brukt for å teste RekrutteringsbistandStatusoppdatering og KvpAvsluttetKafkaDTO.
     */
    @Bean
    <T extends NavCommonKafkaSerialized> KafkaJsonTemplate<T> navCommonKafkaJsonTemplate(ProducerFactory<String, T> navCommonJsonProducerFactory) {
        return new KafkaJsonTemplate<>(navCommonJsonProducerFactory);
    }

    @Bean
    public Admin kafkaAdminClient(KafkaProperties properties, EmbeddedKafkaBroker embeddedKafkaBroker) {
        Map<String, Object> config = properties.buildAdminProperties(new DefaultSslBundleRegistry());
        config.put("bootstrap.servers", embeddedKafkaBroker.getBrokersAsString());
        return Admin.create(config);
    }

    @Bean
    Properties aktivitetskortConsumerProperties(EmbeddedKafkaBroker embeddedKafka) {
        return KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties()
                .withConsumerGroupId(NavCommonKafkaConfig.CONSUMER_GROUP_ID)
                .withBrokerUrl(embeddedKafka.getBrokersAsString())
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .withPollProperties(10, 30_000)
                .build();
    }

    @Bean
    Properties consumerProperties(@Value("${app.kafka.consumer-group-id}") String consumerGroupId, EmbeddedKafkaBroker embeddedKafka) {
        return KafkaPropertiesBuilder.consumerBuilder()
                .withBaseProperties()
                .withConsumerGroupId(consumerGroupId)
                .withBrokerUrl(embeddedKafka.getBrokersAsString())
                .withDeserializers(ByteArrayDeserializer.class, ByteArrayDeserializer.class)
                .withPollProperties(1, 500)
                .build();
    }

    @Bean
    Properties aivenProducerProperties(@Value("${app.kafka.producer-client-id}") String producerClientId, EmbeddedKafkaBroker embeddedKafkaBroker) {
        return KafkaPropertiesBuilder.producerBuilder()
                .withBaseProperties()
                .withProducerId(producerClientId)
                .withBrokerUrl(embeddedKafkaBroker.getBrokersAsString())
                .withSerializers(StringSerializer.class, StringSerializer.class)
                .build();
    }

    @Bean
    public KafkaProducerClient<String, String> producerClient(Properties aivenProducerProperties, MeterRegistry meterRegistry) {
        return KafkaProducerClientBuilder.<String, String>builder()
                .withMetrics(meterRegistry)
                .withProperties(aivenProducerProperties)
                .build();
    }
}
