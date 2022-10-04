package no.nav.veilarbaktivitet.aktivitetskort;

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

import java.util.function.Supplier;

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
    }

    public KafkaAktivitetWrapperDTO fromJson(String json) {
        try {
            objectMapper.readValue(json, KafkaAktivitetWrapperDTO.class);
        } catch (Throwable throwable) {
            throw new DeserialiseringsFeil("jackson ew", throwable.getCause());
        }
    }


    @Override
    public ConsumeStatus consume(ConsumerRecord<String, String> consumerRecord) {
        return consumeWithFeilhandtering(() -> {
            KafkaAktivitetWrapperDTO kafkaAktivitetWrapperDTO = null;
            try {
                kafkaAktivitetWrapperDTO = objectMapper.readValue(consumerRecord.value(), KafkaAktivitetWrapperDTO.class);
            } catch (Throwable throwable) {
            }
            MDC.put(MetricService.SOURCE, kafkaAktivitetWrapperDTO.source);

            if (aktivitetskortService.harSettMelding(kafkaAktivitetWrapperDTO.messageId)) {
                log.warn("Previously handled message seen {} , ignoring", kafkaAktivitetWrapperDTO.messageId);
                return ConsumeStatus.OK;
            } else {
                aktivitetskortService.lagreMeldingsId(
                        kafkaAktivitetWrapperDTO.messageId,
                        kafkaAktivitetWrapperDTO.funksjonellId()
                );
            }
            if (kafkaAktivitetWrapperDTO instanceof KafkaTiltaksAktivitet aktivitet) {
                aktivitetskortService.upsertAktivitetskort(aktivitet.payload);
            } else {
                throw new NotImplementedException("Unknown kafka message");
            }
            return ConsumeStatus.OK;
        });
    }

    private ConsumeStatus consumeWithFeilhandtering(Supplier<ConsumeStatus> block) {
        try {
            return block.get();
        }
        /*catch (AktivitetsKortFunksjonellException e) {
            feilProducer.publishAktivitetsFeil(e,  message.messageId,  message.funksjonellId());
        } */finally {
            MDC.clear();
            return ConsumeStatus.OK;
        }
    }
}
