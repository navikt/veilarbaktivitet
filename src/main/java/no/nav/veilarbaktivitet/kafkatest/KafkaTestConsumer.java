package no.nav.veilarbaktivitet.kafkatest;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avro.ForesporselOmDelingAvCv;
import org.springframework.kafka.annotation.KafkaListener;
@Slf4j
public class KafkaTestConsumer {

    @KafkaListener(topics = "veilarbaktivitet-test-toppic")
    public void consumerKafkaTest(ForesporselOmDelingAvCv data) {
        log.info("KafkaTest - bestillingsId: {}, data: {}", data.getBestillingsId(), data);
    }
}
