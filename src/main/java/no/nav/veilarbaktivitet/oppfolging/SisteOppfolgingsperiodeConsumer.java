package no.nav.veilarbaktivitet.oppfolging;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SisteOppfolgingsperiodeConsumer {
    @KafkaListener(topics = "${topic.inn.sisteOppfolgingsperiode}", containerFactory = "stringStringKafkaListenerContainerFactory")
    public void opprettEllerOppdaterSistePeriode(ConsumerRecord<String, String> consumerRecord) {
        SisteOppfolgingsperiodeV1 sisteOppfolgingsperiodeV1 = JsonUtils.fromJson(consumerRecord.value(), SisteOppfolgingsperiodeV1.class);
        log.info("Siste oppfølgingsperiode: {}", sisteOppfolgingsperiodeV1);


    }
}
