
package no.nav.veilarbaktivitet.config;

import no.nav.common.abac.Pep;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.job.leader_election.LeaderElectionClient;
import no.nav.common.kafka.util.KafkaPropertiesBuilder;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.veilarbaktivitet.config.kafka.KafkaOnpremProperties;
import no.nav.veilarbaktivitet.kvp.KvpClient;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.mock.MetricsClientMock;
import no.nav.veilarbaktivitet.mock.PepMock;
import okhttp3.OkHttpClient;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.*;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

import static org.mockito.Mockito.mock;


@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
public class ApplicationTestConfig {
    @Bean
    /**
     * OkHttpClient uten SystemUserOidcTokenProviderInterceptor. Se  {@link no.nav.veilarbaktivitet.config.ClientConfig}
     */
    public OkHttpClient client() {
        return new OkHttpClient();
    }

    @Bean
    public KvpClient kvpClient() {
        return mock(KvpClient.class);
    }

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider() {
        SystemUserTokenProvider systemUserTokenProvider = mock(SystemUserTokenProvider.class);
        Mockito.when(systemUserTokenProvider.getSystemUserToken()).thenReturn("mockSystemUserToken");
        return systemUserTokenProvider;
    }

    @Bean
    public AuthContextHolder authContextHolder() {
        return AuthContextHolderThreadLocal.instance();
    }

    @Bean
    public Credentials serviceUserCredentials() {
        return new Credentials("username", "password");
    }



    @Bean
    public MetricsClient metricsClient() {
        return new MetricsClientMock();
    }

    @Bean
    public JmsTemplate varselQueue() {
        return mock(JmsTemplate.class);
    }

    @Bean
    public LeaderElectionClient leaderElectionClient() {
        return () -> true;
    }

    @Bean
    public DataSource dataSource() {
        return LocalH2Database.getPresistentDb().getDataSource();
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return LocalH2Database.getPresistentDb();
    }

    @Bean
    public Pep veilarbPep() {
        return new PepMock(null);
    }

    @Bean
    public EmbeddedKafkaBroker embeddedKafka(@Value("${topic.inn.stillingFraNav}") String innTopic, @Value("${topic.ut.stillingFraNav}") String utTopic) {
        return new EmbeddedKafkaBroker(1, true, 1, innTopic, utTopic, "oppfolgingAvsluttetTopic");
    }


    <V extends SpecificRecordBase> ProducerFactory<String, V> avroProducerFactory(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafka) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    <V> ProducerFactory<String, V> jsonProducerFactory(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafka) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    @Bean
    @Primary
    <V extends SpecificRecordBase> KafkaTemplate<String, V> kafkaAvroTemplate(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafka) {
        return new KafkaTemplate<>(avroProducerFactory(kafkaProperties, embeddedKafka));
    }

    @Bean(name = "json")
    KafkaTemplate<String, String> kafkaJsonTemplate(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafka) {
        return new KafkaTemplate<>(jsonProducerFactory(kafkaProperties, embeddedKafka));
    }


    @Bean
    public ConsumerFactory<?, ?> consumerFactory(KafkaProperties kafkaProperties, EmbeddedKafkaBroker embeddedKafka) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
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