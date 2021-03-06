package no.nav.veilarbaktivitet.service;

import io.micrometer.core.annotation.Counted;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.utils.IdUtils;
import no.nav.veilarbaktivitet.aktiviteter_til_kafka.KafkaAktivitetMeldingV4;
import no.nav.veilarbaktivitet.config.kafka.KafkaOnpremProperties;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import static no.nav.common.kafka.producer.util.ProducerUtils.toJsonProducerRecord;
import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaProducerService {

    private final KafkaOnpremProperties kafkaOnpremProperties;

    private final KafkaProducerClient<String, String> producerClient;

    @Counted
    @SneakyThrows
    public long sendAktivitetMelding(KafkaAktivitetMeldingV4 melding) {
        String key = melding.getAktivitetId();

        ProducerRecord<String, String> kafkaMelding = toJsonProducerRecord(kafkaOnpremProperties.getEndringPaaAktivitetTopic(), key, melding);
        kafkaMelding.headers().add(new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, getCorrelationId().getBytes()));

        return producerClient.sendSync(kafkaMelding).offset();
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
