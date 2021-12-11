package no.nav.veilarbaktivitet.config.kafka;

import lombok.RequiredArgsConstructor;
import no.nav.common.kafka.producer.KafkaProducerClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaHelsesjekOppdaterer {
    private final KafkaProducerClient<String, String> producerClient;
    private final KafkaOnpremProperties kafkaOnpremProperties;
    private final KafkaHelsesjekk helsesjekk;

    @Scheduled(
            initialDelay = 1000,
            fixedRate = 1000
    )
    public void uppdateKafkaHelsesjek() {
        try {
            producerClient.getProducer().partitionsFor(kafkaOnpremProperties.getEndringPaaAktivitetTopic());
            helsesjekk.setIsHealty(true, "");
        } catch (Throwable t) {
            helsesjekk.setIsHealty(false, t.getMessage());
        }
    }
}
