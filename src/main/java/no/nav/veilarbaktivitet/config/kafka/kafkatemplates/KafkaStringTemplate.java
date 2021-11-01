package no.nav.veilarbaktivitet.config.kafka.kafkatemplates;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

public class KafkaStringTemplate extends KafkaTemplate<String, String> {
    public KafkaStringTemplate(ProducerFactory<String, String> producerFactory) {
        super(producerFactory);
    }
}
