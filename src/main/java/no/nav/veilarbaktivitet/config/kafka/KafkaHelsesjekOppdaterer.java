package no.nav.veilarbaktivitet.config.kafka;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaHelsesjekOppdaterer {
    private final KafkaStringTemplate kafkaTemplate;

    private final KafkaHelsesjekk helsesjekk;

    @Value("${topic.ut.portefolje}")
    private String portefolgeTopic;

//    @Scheduled(
//            initialDelay = 1000,
//            fixedRate = 1000
//    )
    public void uppdateKafkaHelsesjek() {
        try {
            kafkaTemplate.partitionsFor(portefolgeTopic);
            helsesjekk.setIsHealty(true, "");
        } catch (Exception t) {
            helsesjekk.setIsHealty(false, t.getMessage());
        }
    }

}
