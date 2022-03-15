package no.nav.veilarbaktivitet.config.kafka.kafkatemplates;

import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

public class KafkaAvroAvroTemplate<K extends  SpecificRecordBase, V extends SpecificRecordBase> extends KafkaTemplate<K, V> {
    public KafkaAvroAvroTemplate(ProducerFactory<K, V> producerFactory) {
        super(producerFactory);
    }
}
