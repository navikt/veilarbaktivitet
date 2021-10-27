package no.nav.veilarbaktivitet.config.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

public class KafkaJsonTemplate<K, V> extends KafkaTemplate<K, V> {
    public KafkaJsonTemplate(ProducerFactory<K, V> producerFactory) {
        super(producerFactory);
    }
}
