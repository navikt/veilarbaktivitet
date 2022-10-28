package no.nav.veilarbaktivitet.veilarbportefolje;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.IdUtils;
import no.nav.veilarbaktivitet.aktivitet.AktivitetService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaJsonTemplate;
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import static no.nav.common.kafka.producer.util.ProducerUtils.toJsonProducerRecord;
import static no.nav.common.rest.filter.LogRequestFilter.NAV_CALL_ID_HEADER_NAME;

@Slf4j
@RequiredArgsConstructor
@Service
public class AktivitetKafkaProducerService {

    private final KafkaStringTemplate portefoljeProducer;
    private final KafkaJsonTemplate<AktivitetData> aktivitetProducer;
    private final AktivitetService aktivitetService;
    private final KafkaAktivitetDAO dao;

    @Value("${topic.ut.portefolje}")
    private String portefoljeTopic;

    @Value("${topic.ut.aktivitetdata.rawjson}")
    private String aktivitetTopic;

    @Timed("aktivitet_til_kafka")
    @SneakyThrows
    public void sendAktivitetMelding(KafkaAktivitetMeldingV4 melding) {
        AktivitetData aktivitetData = aktivitetService.hentAktivitetMedFHOForVersion(melding.getVersion());
        ProducerRecord<String, String> portefoljeMelding = toJsonProducerRecord(portefoljeTopic, melding.getAktorId(), melding);
        portefoljeMelding.headers().add(new RecordHeader(NAV_CALL_ID_HEADER_NAME, getCorrelationId().getBytes()));

        ListenableFuture<SendResult<String, AktivitetData>> send = aktivitetProducer.send(aktivitetTopic, aktivitetData.getAktorId(), aktivitetData);
        long offset = portefoljeProducer.send(portefoljeMelding).get().getRecordMetadata().offset();
        send.get();

        dao.updateSendtPaKafkaAven(melding.getVersion(), offset);
    }

    static String getCorrelationId() {
        String correlationId = MDC.get(NAV_CALL_ID_HEADER_NAME);

        if (correlationId == null) {
            correlationId = MDC.get("jobId");
        }
        if (correlationId == null) {
            correlationId = IdUtils.generateId();
        }

        return correlationId;
    }

}
