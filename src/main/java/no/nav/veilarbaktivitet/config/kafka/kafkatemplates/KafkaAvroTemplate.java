package no.nav.veilarbaktivitet.config.kafka.kafkatemplates;

import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

public class KafkaAvroTemplate<V extends SpecificRecordBase> extends KafkaTemplate<String, V> {
    public KafkaAvroTemplate(ProducerFactory<String, V> producerFactory) {
        super(producerFactory);
    }
}
