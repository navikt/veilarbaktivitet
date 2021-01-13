package no.nav.veilarbaktivitet.aktiviterTilKafka;

import io.micrometer.core.annotation.Counted;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.utils.IdUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Slf4j
@Service
@AllArgsConstructor
public class KafkaServiceImpl implements KafkaService {
    private Producer<String, String> producer;

    @Counted
    @SneakyThrows
    public long sendMeldingV4(KafkaAktivitetMeldingV4 meldingV4) {
        String key = meldingV4.getAktivitetId();
        String correlationId = getCorrelationId();
        ProducerRecord<String, String> record = new ProducerRecord<>(KafkaConfig.KAFKA_TOPIC_AKTIVITETER_V4, key, JsonUtils.toJson(meldingV4));
        record.headers().add(new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, correlationId.getBytes()));
        RecordMetadata recordMetadata = producer.send(record).get();
        log.info("Sender aktivitet {}, version {} på kafka med callId {} for bruker med aktørId {} på topic {} ofcet {}", meldingV4.getAktivitetId(), meldingV4.getVersion(), correlationId, meldingV4.getAktorId(), KafkaConfig.KAFKA_TOPIC_AKTIVITETER_V4, recordMetadata.offset());
        return recordMetadata.offset();
    }

    @Counted
    @SneakyThrows
    public long sendMelding(KafkaAktivitetMeldingV3 melding) {
        String key = melding.getAktivitetId();
        String correlationId = getCorrelationId();
        ProducerRecord<String, String> record = new ProducerRecord<>(KafkaConfig.KAFKA_TOPIC_AKTIVITETER_V3, key, JsonUtils.toJson(melding));
        record.headers().add(new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, correlationId.getBytes()));
        RecordMetadata recordMetadata = producer.send(record).get();
        log.info("Sender aktivitet {}, version {} på kafka med callId {} for bruker med aktørId {} på topic {} ofcet {}", melding.getAktivitetId(), melding.getVersion(), correlationId, melding.getAktorId(), KafkaConfig.KAFKA_TOPIC_AKTIVITETER_V3, recordMetadata.offset());
        return recordMetadata.offset();
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
