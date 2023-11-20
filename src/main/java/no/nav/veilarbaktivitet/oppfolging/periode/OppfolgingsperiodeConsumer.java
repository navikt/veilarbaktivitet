package no.nav.veilarbaktivitet.oppfolging.periode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class OppfolgingsperiodeConsumer {
    private final OppfolgingsperiodeService oppfolgingsperiodeService;

    @KafkaListener(topics = "${topic.inn.oppfolgingsperiode}", containerFactory = "stringStringKafkaListenerContainerFactory")
    void opprettEllerOppdaterSistePeriode(ConsumerRecord<String, String> consumerRecord) {
        SisteOppfolgingsperiodeV1 sisteOppfolgingsperiodeV1 = JsonUtils.fromJson(consumerRecord.value(), SisteOppfolgingsperiodeV1.class);
        log.info("oppfølgingsperiode: {}", sisteOppfolgingsperiodeV1);
        oppfolgingsperiodeService.upsertOppfolgingsperiode(sisteOppfolgingsperiodeV1);

        if(sisteOppfolgingsperiodeV1.sluttDato != null) {
            oppfolgingsperiodeService.avsluttOppfolgingsperiode(sisteOppfolgingsperiodeV1.uuid, sisteOppfolgingsperiodeV1.sluttDato);
        }


    }
}
