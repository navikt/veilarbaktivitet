package no.nav.veilarbaktivitet.oppfolging.siste_periode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class SisteOppfolgingsperiodeConsumer {
    private final SistePeriodeDAO sistePeriodeDAO;

//TODO slett groupId = "veilarbaktivitet-temp"
@KafkaListener(topics = "${topic.inn.sisteOppfolgingsperiode}", containerFactory = "stringStringKafkaListenerContainerFactory", groupId = "veilarbaktivitet-temp")
    void opprettEllerOppdaterSistePeriode(ConsumerRecord<String, String> consumerRecord) {
        SisteOppfolgingsperiodeV1 sisteOppfolgingsperiodeV1 = JsonUtils.fromJson(consumerRecord.value(), SisteOppfolgingsperiodeV1.class);
        log.info("Siste oppfølgingsperiode: {}", sisteOppfolgingsperiodeV1);
        sistePeriodeDAO.uppsertOppfolingsperide(
                new Oppfolgingsperiode(
                        sisteOppfolgingsperiodeV1.aktorId,
                        sisteOppfolgingsperiodeV1.uuid,
                        sisteOppfolgingsperiodeV1.startDato,
                        sisteOppfolgingsperiodeV1.sluttDato));
    }
}