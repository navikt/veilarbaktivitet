package no.nav.veilarbaktivitet.aktiviterTilKafka;

import io.micrometer.core.annotation.Counted;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.utils.IdUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Slf4j
@Service
@AllArgsConstructor
public class KafkaServiceImpl implements KafkaService {
    private Producer<String, String> producer;

    @SneakyThrows
    public void sendMelding(KafkaAktivitetMelding melding) {
        String key = melding.getAktorId();
        String correlationId = getCorrelationId();
        ProducerRecord<String, String> record = new ProducerRecord<>(KafkaConfig.KAFKA_TOPIC_AKTIVITETER, key, JsonUtils.toJson(melding));
        record.headers().add(new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, correlationId.getBytes()));
        log.info("Sender aktivitet {} på kafka med callId {} for bruker med aktørId {} på topic {}", melding.getAktivitetId(), correlationId, melding.getAktorId(), KafkaConfig.KAFKA_TOPIC_AKTIVITETER);
        producer.send(record).get();
    }

    @Counted
    @SneakyThrows
    public void sendMeldingV2(KafkaAktivitetMeldingV2 melding) {
        String key = melding.getAktorId();
        String correlationId = getCorrelationId();
        ProducerRecord<String, String> record = new ProducerRecord<>(KafkaConfig.KAFKA_TOPIC_AKTIVITETER_V2, key, JsonUtils.toJson(melding));
        record.headers().add(new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, correlationId.getBytes()));
        log.info("Sender aktivitet {} på kafka med callId {} for bruker med aktørId {} på topic {}", melding.getAktivitetId(), correlationId, melding.getAktorId(), KafkaConfig.KAFKA_TOPIC_AKTIVITETER_V2);
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
