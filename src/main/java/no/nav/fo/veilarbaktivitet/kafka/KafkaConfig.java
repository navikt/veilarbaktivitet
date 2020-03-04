package no.nav.fo.veilarbaktivitet.kafka;

import no.nav.fo.veilarbaktivitet.db.DatabaseContext;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Properties;

import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_PASSWORD;
import static no.nav.sbl.dialogarena.common.abac.pep.CredentialConstants.SYSTEMUSER_USERNAME;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
@Import({DatabaseContext.class})
public class KafkaConfig {

    public static final String KAFKA_TOPIC_AKTIVITETER = getRequiredProperty("KAFKA_TOPIC_AKTIVITETER");
    public static final String KAFKA_BROKERS = getRequiredProperty("KAFKA_BROKERS_URL");

    private static final String USERNAME = getRequiredProperty(SYSTEMUSER_USERNAME);
    private static final String PASSWORD = getRequiredProperty(SYSTEMUSER_PASSWORD);

    public static Properties producerConfig() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "15000");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "veilarbaktivitet-producer");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        properties.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        properties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, generateId());
        return properties;
    }

    @Bean
    public KafkaProducer<String, String> kafkaProducer() {
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerConfig());
        producer.initTransactions();
        return producer;
    }

    @Bean
    public KafkaService kafkaService(JdbcTemplate jdbcTemplate) {
        return new KafkaService(kafkaProducer(), kafkaDAO(jdbcTemplate));
    }

    @Bean
    public KafkaDAO kafkaDAO(JdbcTemplate jdbcTemplate) {
        return new KafkaDAO(jdbcTemplate);
    }
}
