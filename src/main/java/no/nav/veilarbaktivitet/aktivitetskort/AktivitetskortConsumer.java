package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.aktivitetskort.service.AktivitetskortService;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@ToString(of = {"aktivitetskortService"})
public class AktivitetskortConsumer implements TopicConsumer<String, String> {

    private final AktivitetskortService aktivitetskortService;

    public final AktivitetsKortFeilProducer feilProducer;

    @Transactional(noRollbackFor = AktivitetsKortFunksjonellException.class)
    @Override
    public ConsumeStatus consume(ConsumerRecord<String, String> consumerRecord) {
        try {
            return consumeThrowing(consumerRecord);
        } catch (DuplikatMeldingFeil e) {
            return ConsumeStatus.OK;
        } catch (AktivitetsKortFunksjonellException e) {
            log.error("Funksjonell feil i aktivitetkortConumer", e);
            feilProducer.publishAktivitetsFeil(e, consumerRecord);
            return ConsumeStatus.OK;
        } finally {
            MDC.remove(MetricService.SOURCE);
        }
    }

    private final AktivitetsbestillingCreator bestillingsCreator;
    ConsumeStatus consumeThrowing(ConsumerRecord<String, String> consumerRecord) throws AktivitetsKortFunksjonellException {
        var bestilling = bestillingsCreator.lagBestilling(consumerRecord);
        log.info("Konsumerer aktivitetskortmelding: messageId={}, sendt={}, funksjonellId={}", bestilling.getMessageId(), consumerRecord.timestampType(), bestilling.getAktivitetskort().id);
        ignorerHvisSettFor(bestilling.getMessageId(), bestilling.getAktivitetskort().id);

        MDC.put(MetricService.SOURCE, bestilling.getSource());
        if (bestilling.getActionType() == ActionType.UPSERT_AKTIVITETSKORT_V1) {
            aktivitetskortService.upsertAktivitetskort(bestilling);
        } else {
            throw new NotImplementedException("Unknown kafka message");
        }
        return ConsumeStatus.OK;
    }

    private void ignorerHvisSettFor(UUID messageId, UUID funksjonellId) throws DuplikatMeldingFeil {
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
}
