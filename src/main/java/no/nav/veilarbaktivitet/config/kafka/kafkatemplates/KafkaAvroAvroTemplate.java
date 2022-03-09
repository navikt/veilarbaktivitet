package no.nav.veilarbaktivitet.config.kafka.kafkatemplates;

import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

public class KafkaAvroAvroTemplate<k extends  SpecificRecordBase, V extends SpecificRecordBase> extends KafkaTemplate<k, V> {
    public KafkaAvroAvroTemplate(ProducerFactory<k, V> producerFactory) {
        super(producerFactory);
    }
}
