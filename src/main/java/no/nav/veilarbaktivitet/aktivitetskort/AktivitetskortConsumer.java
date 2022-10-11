package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonMapper;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@ToString(of = {"aktivitetskortService"})
public class AktivitetskortConsumer implements TopicConsumer<String, String> {

    private final AktivitetskortService aktivitetskortService;

    public final AktivitetsKortFeilProducer feilProducer;

    private static final ObjectMapper objectMapper = JsonMapper.defaultObjectMapper();

    public AktivitetskortConsumer(
        AktivitetskortService aktivitetskortService,
        AktivitetsKortFeilProducer feilProducer
    ) {
        super();
        this.aktivitetskortService = aktivitetskortService;
        this.feilProducer = feilProducer;
        objectMapper.registerSubtypes(TiltaksaktivitetDTO.class, KafkaAktivitetWrapperDTO.class);
        objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public KafkaAktivitetWrapperDTO deserialiser(ConsumerRecord<String, String> record) throws DeserialiseringsFeil {
        try {
            return objectMapper.readValue(record.value(), KafkaAktivitetWrapperDTO.class);
        } catch (Throwable throwable) {
            throw new DeserialiseringsFeil(
                    new ErrorMessage(throwable.getMessage()),
                    throwable
            );
        }
    }

    @Transactional(noRollbackFor = AktivitetsKortFunksjonellException.class)
    @Override
    public ConsumeStatus consume(ConsumerRecord<String, String> record) {
        try {
            return consumeThrowing(record);
        } catch (DuplikatMeldingFeil e) {
            return ConsumeStatus.OK;
        } catch (AktivitetsKortFunksjonellException e) {
            feilProducer.publishAktivitetsFeil(e, record);
            return ConsumeStatus.OK;
        } finally {
            MDC.remove(MetricService.SOURCE);
        }
    }

    ConsumeStatus consumeThrowing(ConsumerRecord<String, String> record) throws AktivitetsKortFunksjonellException {
        var melding = deserialiser(record);
        ignorerHvisSettFør(melding);

        MDC.put(MetricService.SOURCE, melding.source);
        if (melding instanceof KafkaTiltaksAktivitet aktivitet) {
            aktivitetskortService.upsertAktivitetskort(aktivitet.payload);
        } else {
            throw new NotImplementedException("Unknown kafka message");
        }
        return ConsumeStatus.OK;
    }

    private void ignorerHvisSettFør(KafkaAktivitetWrapperDTO message) throws DuplikatMeldingFeil {
        if (aktivitetskortService.harSettMelding(message.messageId)) {
            log.warn("Previously handled message seen {} , ignoring", message.messageId);
            throw new DuplikatMeldingFeil();
        } else {
            aktivitetskortService.lagreMeldingsId(
                message.messageId,
                message.funksjonellId()
            );
        }
    }
}
