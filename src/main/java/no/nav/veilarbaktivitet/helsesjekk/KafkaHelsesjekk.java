package no.nav.veilarbaktivitet.helsesjekk;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.stereotype.Component;

import static no.nav.veilarbaktivitet.aktiviterTilKafka.KafkaConfig.KAFKA_TOPIC_AKTIVITETER_V4;

@Component
public class KafkaHelsesjekk implements HealthCheck {

    private KafkaProducer<String, String> kafka;

    public KafkaHelsesjekk(KafkaProducer<String, String> kafka) {
        this.kafka = kafka;
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            kafka.partitionsFor(KAFKA_TOPIC_AKTIVITETER_V4);
        } catch (Throwable t){
            return HealthCheckResult.unhealthy("Helsesjekk feilet mot kafka feilet", t);
        }
        return HealthCheckResult.healthy();
    }
}
