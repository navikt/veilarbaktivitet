package no.nav.fo.veilarbaktivitet.kafka;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.IdUtils;
import no.nav.json.JsonUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.fo.veilarbaktivitet.kafka.KafkaConfig.KAFKA_TOPIC_AKTIVITETER;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Slf4j
@Component
public class KafkaService {
    private Producer<String, String> producer;

    @Inject
    public KafkaService(Producer<String, String> producer) {
        this.producer = producer;
    }

    @SneakyThrows
    public void sendMelding(KafkaAktivitetMelding melding) {
        String key = melding.getAktorId();
        String correlationId = getCorrelationId();
        ProducerRecord<String, String> record = new ProducerRecord<>(KAFKA_TOPIC_AKTIVITETER, key, JsonUtils.toJson(melding));
        record.headers().add(new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, correlationId.getBytes()));
        log.info("Sender aktivitet {} på kafka med callId {} for bruker med aktørId {}", melding.getAktivitetId(), correlationId, melding.getAktorId());
        producer.send(record).get();
    }

    static String getCorrelationId() {
        String correlationId = MDC.get(PREFERRED_NAV_CALL_ID_HEADER_NAME);

        if (correlationId == null) {
            correlationId = MDC.get("jobId");
        }
        if (correlationId == null) {
            correlationId = IdUtils.generateId();
        }

        return correlationId;
    }
}
