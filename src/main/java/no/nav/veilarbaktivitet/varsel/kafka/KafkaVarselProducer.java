package no.nav.veilarbaktivitet.varsel.kafka;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.utils.IdUtils;
import no.nav.veilarbaktivitet.varsel.event.VarselEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaVarselProducer {

    @Value("${SEND_VARSEL_TOPIC:privat-fo-varsel-q1}")
    private String topic;

    private final KafkaProducerClient<String, VarselEvent> producer;

    @Timed
    @SneakyThrows
    public long send(String key, VarselEvent value) {
        ProducerRecord<String, VarselEvent> producerRecord = new ProducerRecord<>(topic, key, value);
        producerRecord.headers().add(new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, getCorrelationId().getBytes()));

        return producer.sendSync(producerRecord).offset();
    }

    private static String getCorrelationId() {
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
