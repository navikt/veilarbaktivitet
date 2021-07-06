package no.nav.veilarbaktivitet.kafkatest;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avro.ForesporselOmDelingAvCv;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaTestConsumer {

    @KafkaListener(topics = "pto.veilarbaktivitet-test-toppic")
    public void consumerKafkaTest(ForesporselOmDelingAvCv data) {
        log.info("KafkaTest - bestillingsId: {}, data: {}", data.getBestillingsId(), data);
    }
}
