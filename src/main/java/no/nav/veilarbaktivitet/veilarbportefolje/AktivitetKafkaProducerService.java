package no.nav.veilarbaktivitet.veilarbportefolje;

import io.micrometer.core.annotation.Counted;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.IdUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.config.kafka.KafkaJsonTemplate;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static no.nav.common.kafka.producer.util.ProducerUtils.toJsonProducerRecord;
import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Slf4j
@RequiredArgsConstructor
@Service
public class AktivitetKafkaProducerService {

    private final KafkaTemplate<String, String> portefoljeProducer;
    private final KafkaJsonTemplate<String, AktivitetData> aktivitetProducer;

    @Value("${topic.ut.portefolje}")
    private String portefoljeTopic;

    @Value("${topic.ut.aktivitetdata.rawjson}")
    private String aktivitetTopic;

    @Counted
    @SneakyThrows
    public long sendAktivitetMelding(KafkaAktivitetMeldingV4 melding, AktivitetData aktivitetData) {
        ProducerRecord<String, String> portefoljeMelding = toJsonProducerRecord(portefoljeTopic, melding.getAktorId(), melding);
        portefoljeMelding.headers().add(new RecordHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME, getCorrelationId().getBytes()));

        aktivitetProducer.send(aktivitetTopic, aktivitetData.getAktorId(), aktivitetData).get();

        return portefoljeProducer.send(portefoljeMelding).get().getRecordMetadata().offset();
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
