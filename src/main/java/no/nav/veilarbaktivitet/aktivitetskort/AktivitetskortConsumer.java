package no.nav.veilarbaktivitet.aktivitetskort;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.aktivitetskort.dto.BestillingBase;
import no.nav.veilarbaktivitet.aktivitetskort.dto.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.dto.bestilling.KasseringsBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.feil.*;
import no.nav.veilarbaktivitet.aktivitetskort.service.AktivitetskortService;
import no.nav.veilarbaktivitet.aktivitetskort.service.KasseringsService;
import no.nav.veilarbaktivitet.aktivitetskort.service.UpsertActionResult;
import no.nav.veilarbaktivitet.aktivitetskort.util.JsonUtil;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@ToString(of = {"aktivitetskortService"})
public class AktivitetskortConsumer implements TopicConsumer<String, String> {

    public static final String UNIQUE_MESSAGE_IDENTIFIER = "messageId";
    private final AktivitetskortService aktivitetskortService;
    private final KasseringsService kasseringsService;

    private final AktivitetsKortFeilProducer feilProducer;

    private final AktivitetskortMetrikker aktivitetskortMetrikker;

    @Transactional(noRollbackFor = AktivitetsKortFunksjonellException.class)
    @Override
    @Timed(value="akas_consume_aktivitetskort")
    public ConsumeStatus consume(ConsumerRecord<String, String> consumerRecord) {
        UUID messageId = null;
        try {
            messageId = extractMessageId(consumerRecord);
            UUID funksjonellId = UUID.fromString(consumerRecord.key()); // Lik aktivitetskort.id i payload
            ignorerHvisSettFor(messageId, funksjonellId);
            if (messageId.equals(funksjonellId)) {
                throw new MessageIdIkkeUnikFeil(new ErrorMessage("messageId må være unik for hver melding. aktivitetsId er lik messageId"), null);
            }
            var bestilling = bestillingsCreator.lagBestilling(consumerRecord);
            if (bestilling.getMessageId() == null) {
                bestilling.setMessageId(messageId); // messageId populert i header i stedet for payload
            }
            MDC.put(MetricService.SOURCE, bestilling.getSource());

            var timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(consumerRecord.timestamp()), ZoneId.systemDefault());
            log.info("Konsumerer aktivitetskortmelding: offset={}, partition={}, messageId={}, sendt={}, funksjonellId={}", consumerRecord.offset(), consumerRecord.partition(), bestilling.getMessageId(), timestamp, bestilling.getAktivitetskortId());

            return behandleBestilling(bestilling);
        } catch (DuplikatMeldingFeil e) {
            return ConsumeStatus.OK;
        } catch (AktivitetsKortFunksjonellException e) {
            log.error("Funksjonell feil {} i aktivitetkortConumer for aktivitetskort_v1 offset={} partition={}", e.getMessage(), consumerRecord.offset(), consumerRecord.partition());
            if (messageId == null) {
                log.error("MessageId mangler for aktivitetskort melding med key {}. Får ikke oppdatert meldingsresultat.", consumerRecord.key());
            } else {
                aktivitetskortService.oppdaterMeldingResultat(messageId, UpsertActionResult.FUNKSJONELL_FEIL, e.getClass().getSimpleName());
            }
            feilProducer.publishAktivitetsFeil(e, consumerRecord);
            return ConsumeStatus.OK;
        } catch (Exception e) {
            aktivitetskortMetrikker.countAktivitetskortTekniskFeil();
            throw e;
        } finally {
            MDC.remove(MetricService.SOURCE);
        }
    }


    private final AktivitetsbestillingCreator bestillingsCreator;
    ConsumeStatus behandleBestilling(BestillingBase bestilling) throws AktivitetsKortFunksjonellException {

        if (bestilling instanceof AktivitetskortBestilling aktivitetskortBestilling) {
            UpsertActionResult upsertActionResult = aktivitetskortService.upsertAktivitetskort(aktivitetskortBestilling);
            aktivitetskortService.oppdaterMeldingResultat(aktivitetskortBestilling.getMessageId(), upsertActionResult, null);
            aktivitetskortMetrikker.countAktivitetskortUpsert(aktivitetskortBestilling, upsertActionResult);
        } else if (bestilling instanceof KasseringsBestilling kasseringsBestilling) {
            kasseringsService.kassertAktivitet(kasseringsBestilling);
            aktivitetskortService.oppdaterMeldingResultat(kasseringsBestilling.getMessageId(), UpsertActionResult.KASSER, null);
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

    private UUID extractMessageId(ConsumerRecord<String, String> consumerRecord) throws AktivitetsKortFunksjonellException {

        try {
            String messageId;

            messageId = JsonUtil.extractStringPropertyFromJson(UNIQUE_MESSAGE_IDENTIFIER, consumerRecord.value());
            if (messageId != null) {
                return UUID.fromString(messageId);
            }


            Header messageIdHeader = consumerRecord.headers().lastHeader(UNIQUE_MESSAGE_IDENTIFIER);
            if (messageIdHeader != null) {
                messageId = new String(messageIdHeader.value());
                return UUID.fromString(messageId);
            }

            throw new RuntimeException("Mangler påkrevet messageId på aktivitetskort melding");
        } catch (IOException e) {
            throw new DeserialiseringsFeil(new ErrorMessage("Meldingspayload er ikke gyldig json"), e);
        } catch (IllegalArgumentException e) {
            throw new DeserialiseringsFeil(new ErrorMessage("MessageId er ikke en gyldig UUID verdi"), e);
        }
    }
}
