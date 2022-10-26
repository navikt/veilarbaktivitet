package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonMapper;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

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
        objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public KafkaAktivitetskortWrapperDTO deserialiser(ConsumerRecord<String, String> consumerRecord) throws DeserialiseringsFeil {
        try {
            return objectMapper.readValue(consumerRecord.value(), KafkaAktivitetskortWrapperDTO.class);
        } catch (Exception e) {
            throw new DeserialiseringsFeil(
                    new ErrorMessage(e.getMessage()),
                    e
            );
        }
    }

    @Transactional(noRollbackFor = AktivitetsKortFunksjonellException.class)
    @Override
    public ConsumeStatus consume(ConsumerRecord<String, String> consumerRecord) {
        try {
            return consumeThrowing(consumerRecord);
        } catch (DuplikatMeldingFeil e) {
            return ConsumeStatus.OK;
        } catch (AktivitetsKortFunksjonellException e) {
            feilProducer.publishAktivitetsFeil(e, consumerRecord);
            return ConsumeStatus.OK;
        } finally {
            MDC.remove(MetricService.SOURCE);
        }
    }

    ConsumeStatus consumeThrowing(ConsumerRecord<String, String> consumerRecord) throws AktivitetsKortFunksjonellException {
        var melding = deserialiser(consumerRecord);
        log.info("Konsumerer aktivitetskortmelding: messageId={}, sendt={}, funksjonellId={}", melding.messageId, melding.sendt, melding.aktivitetskort.id);
        ignorerHvisSettFør(melding.messageId, melding.aktivitetskort.id);

        MeldingContext meldingContext = new MeldingContext(
                getEksternReferanseId(consumerRecord),
                getArenaTiltakskode(consumerRecord),
                melding.source,
                melding.aktivitetskortType
        );

        boolean erArenaAktivitet = "ARENA_TILTAK_AKTIVITET_ACL".equals(melding.source);
        MDC.put(MetricService.SOURCE, melding.source);
        if (melding.actionType == ActionType.UPSERT_AKTIVITETSKORT_V1) {
            aktivitetskortService.upsertAktivitetskort(melding.aktivitetskort, meldingContext, erArenaAktivitet);
        } else {
            throw new NotImplementedException("Unknown kafka message");
        }
        return ConsumeStatus.OK;
    }

    private void ignorerHvisSettFør(UUID messageId, UUID funksjonellId) throws DuplikatMeldingFeil {
        if (aktivitetskortService.harSettMelding(messageId)) {
            log.warn("Previously handled message seen {} , ignoring", messageId);
            throw new DuplikatMeldingFeil();
        } else {
            aktivitetskortService.lagreMeldingsId(
                messageId,
                funksjonellId
            );
        }
    }

    private ArenaId getEksternReferanseId(ConsumerRecord<String, String> consumerRecord) {
        byte[] eksternReferanseIdBytes = consumerRecord.headers().lastHeader("eksternReferanseId").value();
        return new ArenaId(new String(eksternReferanseIdBytes));
    }

    private String getArenaTiltakskode(ConsumerRecord<String, String> consumerRecord) {
        byte[] arenaTiltakskode = consumerRecord.headers().lastHeader("arenaTiltakskode").value();
        return new String(arenaTiltakskode);
    }

}
