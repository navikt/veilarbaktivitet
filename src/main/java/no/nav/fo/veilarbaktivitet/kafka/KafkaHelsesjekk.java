package no.nav.fo.veilarbaktivitet.kafka;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.fo.veilarbaktivitet.kafka.KafkaConfig.KAFKA_TOPIC_AKTIVITETER;
import static no.nav.fo.veilarbaktivitet.kafka.KafkaConfig.KAFKA_BROKERS;

@Component
public class KafkaHelsesjekk implements Helsesjekk {

    private KafkaProducer<String, String> kafka;

    @Inject
    public KafkaHelsesjekk(KafkaProducer<String, String> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void helsesjekk() throws Throwable {
        kafka.partitionsFor(KAFKA_TOPIC_AKTIVITETER);
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka", KAFKA_BROKERS, KAFKA_TOPIC_AKTIVITETER, false);
    }


}
