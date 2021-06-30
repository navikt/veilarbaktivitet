package no.nav.veilarbaktivitet.helsesjekk;

import lombok.RequiredArgsConstructor;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.veilarbaktivitet.config.kafka.KafkaOnpremProperties;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class KafkaHelsesjekk implements HealthCheck {

    private final KafkaProducerClient<String, String> producerClient;

    private final KafkaOnpremProperties kafkaOnpremProperties;

    @Override
    public HealthCheckResult checkHealth() {
        try {
            producerClient.getProducer().partitionsFor(kafkaOnpremProperties.getEndringPaaAktivitetTopic());
        } catch (Throwable t) {
            return HealthCheckResult.unhealthy("Helsesjekk feilet mot kafka feilet", t);
        }

        return HealthCheckResult.healthy();
    }
}
