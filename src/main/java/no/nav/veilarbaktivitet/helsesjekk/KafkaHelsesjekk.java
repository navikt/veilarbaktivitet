package no.nav.veilarbaktivitet.helsesjekk;

import lombok.RequiredArgsConstructor;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.veilarbaktivitet.config.KafkaProperties;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class KafkaHelsesjekk implements HealthCheck {

    private final KafkaProducerClient<String, String> kafkaProducerClient;

    private final KafkaProperties kafkaProperties;

    @Override
    public HealthCheckResult checkHealth() {
        try {
            kafkaProducerClient.getProducer().partitionsFor(kafkaProperties.getEndringPaaAktivitetTopic());
        } catch (Throwable t) {
            return HealthCheckResult.unhealthy("Helsesjekk feilet mot kafka feilet", t);
        }

        return HealthCheckResult.healthy();
    }
}
