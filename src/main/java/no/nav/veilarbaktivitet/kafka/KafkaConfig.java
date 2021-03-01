package no.nav.veilarbaktivitet.kafka;

import no.nav.common.utils.Credentials;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.LoggingProducerListener;

import java.util.HashMap;

import static no.nav.veilarbaktivitet.kafka.KafkaTopics.requireKafkaTopicEnv;

@EnableKafka
@Configuration
public class KafkaConfig {

    public final static String CONSUMER_GROUP_ID = "veilarbaktivitet-consumer";

    public final static String PRODUCER_GROUP_ID = "veilarbaktivitet-producer";

    private final Credentials serviceUserCredentials;

    @Value("${spring.kafka.bootstrap-servers}")
    String brokersUrl;

    @Autowired
    public KafkaConfig(Credentials serviceUserCredentials) {
        this.serviceUserCredentials = serviceUserCredentials;
    }

    @Bean
    public KafkaTopics kafkaTopics() {
        return KafkaTopics.create(requireKafkaTopicEnv());
    }

    @Bean
    public KafkaListenerContainerFactory kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(kafkaConsumerProperties(brokersUrl, serviceUserCredentials)));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(kafkaProducerProperties(brokersUrl, serviceUserCredentials));
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);

        LoggingProducerListener<String, String> producerListener = new LoggingProducerListener<>();
        producerListener.setIncludeContents(false);
        template.setProducerListener(producerListener);

        return template;
    }

    private static HashMap<String, Object> kafkaBaseProperties(String kafkaBrokersUrl, Credentials serviceUserCredentials) {
        HashMap<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokersUrl);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + serviceUserCredentials.username + "\" password=\"" + serviceUserCredentials.password + "\";");
        return props;
    }

    private static HashMap<String, Object> kafkaConsumerProperties(String kafkaBrokersUrl, Credentials serviceUserCredentials) {
        HashMap<String, Object> props = kafkaBaseProperties(kafkaBrokersUrl, serviceUserCredentials);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 30 * 60 * 1000); // 30 minutes
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    private static HashMap<String, Object> kafkaProducerProperties (String kafkaBrokersUrl, Credentials serviceUserCredentials) {
        HashMap<String, Object> props = kafkaBaseProperties(kafkaBrokersUrl, serviceUserCredentials);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, PRODUCER_GROUP_ID);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

}
