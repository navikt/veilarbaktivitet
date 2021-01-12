package no.nav.veilarbaktivitet.aktiviterTilKafka;

import no.nav.common.utils.Credentials;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;

@Configuration
public class KafkaConfig {

    // TODO: fjern denne fra health check
    public static final String KAFKA_TOPIC_AKTIVITETER = getRequiredProperty("KAFKA_TOPIC_AKTIVITETER");
    public static final String KAFKA_TOPIC_AKTIVITETER_V3 = getRequiredProperty("KAFKA_TOPIC_AKTIVITETER_V3");
    // TODO: Legg til topic
    public static final String KAFKA_TOPIC_AKTIVITETER_V4 = getRequiredProperty("KAFKA_TOPIC_AKTIVITETER_V4");
    public static final String KAFKA_BROKERS = getRequiredProperty("KAFKA_BROKERS_URL");

    private final Credentials serviceUserCredentials;

    public KafkaConfig(Credentials serviceUserCredentials) {
        this.serviceUserCredentials = serviceUserCredentials;
    }

    public Properties producerConfig() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "15000");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "veilarbaktivitet-producer");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        properties.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" +  serviceUserCredentials.username + "\" password=\"" + serviceUserCredentials.password + "\";");
        return properties;
    }

    @Bean
    public KafkaProducer<String, String> kafkaProducer() {
        return new KafkaProducer(producerConfig());
    }

}
