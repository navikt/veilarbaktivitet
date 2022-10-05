package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonMapper;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.config.kafka.AivenConsumerConfig;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ToString(of = {"aktivitetskortService"})
public class AktivitetskortConsumer extends AivenConsumerConfig<String, String> implements TopicConsumer<String, String> {

    private final AktivitetskortService aktivitetskortService;

    public final AktivitetsKortFeilProducer feilProducer;

    private static final ObjectMapper objectMapper = JsonMapper.defaultObjectMapper();

    public AktivitetskortConsumer(
            AktivitetskortService aktivitetskortService,
            @Value("${topic.inn.aktivitetskort}")
            String topic,
            AktivitetsKortFeilProducer feilProducer) {
        super();
        this.aktivitetskortService = aktivitetskortService;
        this.feilProducer = feilProducer;
        this.setTopic(topic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(Deserializers.stringDeserializer());
        this.setConsumer(this);
        objectMapper.registerSubtypes(TiltaksaktivitetDTO.class, KafkaAktivitetWrapperDTO.class);
        objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
    }

    public KafkaAktivitetWrapperDTO deserialiser(ConsumerRecord<String, String> record) throws DeserialiseringsFeil {
        try {
            return objectMapper.readValue(record.value(), KafkaAktivitetWrapperDTO.class);
        } catch (Throwable throwable) {
            throw new DeserialiseringsFeil(
                    record.key(),
                    new ErrorMessage("Could not deserialize message"),
                    new FailingMessage(record.value()),
                    throwable
            );
        }
    }


    @Override
    public ConsumeStatus consume(ConsumerRecord<String, String> consumerRecord) {
        return consumeWithFeilhandtering(() -> {
            var melding = deserialiser(consumerRecord);
            ignorerHvisSettFør(melding, consumerRecord.value());

            MDC.put(MetricService.SOURCE, melding.source);
            if (melding instanceof KafkaTiltaksAktivitet aktivitet) {
                aktivitetskortService.upsertAktivitetskort(aktivitet.payload);
            } else {
                throw new NotImplementedException("Unknown kafka message");
            }
            return ConsumeStatus.OK;
        });
    }

    private void ignorerHvisSettFør(KafkaAktivitetWrapperDTO message, String rawMessage) throws DuplikatMeldingFeil {
        if (aktivitetskortService.harSettMelding(message.messageId)) {
            log.warn("Previously handled message seen {} , ignoring", message.messageId);
            throw new DuplikatMeldingFeil(message.funksjonellId(), new FailingMessage(rawMessage));
        } else {
            aktivitetskortService.lagreMeldingsId(
                    message.messageId,
                    message.funksjonellId()
            );
        }
    }

    private ConsumeStatus consumeWithFeilhandtering(MessageConsumer block) {
        try {
            return block.consume();
        } catch (DuplikatMeldingFeil e) {
            return ConsumeStatus.OK;
        } catch (AktivitetsKortFunksjonellException e) {
            feilProducer.publishAktivitetsFeil(e);
        } finally {
            MDC.clear();
            return ConsumeStatus.OK;
        }
    }
}

interface MessageConsumer {
    ConsumeStatus consume() throws AktivitetsKortFunksjonellException;
}