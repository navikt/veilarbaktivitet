package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.veilarbaktivitet.aktivitet.MetricService;
import no.nav.veilarbaktivitet.config.kafka.AivenConsumerConfig;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@Slf4j
@ToString(of = {"aktivitetskortService"})
public class AktivitetskortConsumer extends AivenConsumerConfig<String, KafkaAktivitetWrapperDTO> implements TopicConsumer<String, KafkaAktivitetWrapperDTO> {

    private final AktivitetskortService aktivitetskortService;

    public final AktivitetsKortFeilProducer feilProducer;

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
        this.setValueDeserializer(Deserializers.jsonDeserializer(KafkaAktivitetWrapperDTO.class));
        this.setConsumer(this);
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, KafkaAktivitetWrapperDTO> consumerRecord) {
        KafkaAktivitetWrapperDTO kafkaAktivitetWrapperDTO = consumerRecord.value();

        if (aktivitetskortService.harSettMelding(kafkaAktivitetWrapperDTO.messageId)) {
            log.warn("Previously handled message seen {} , ignoring", kafkaAktivitetWrapperDTO.messageId);
            return ConsumeStatus.OK;
        } else {
            aktivitetskortService.lagreMeldingsId(
                kafkaAktivitetWrapperDTO.messageId,
                kafkaAktivitetWrapperDTO.funksjonellId()
            );
        }

        return consumeWithFeilhandtering(() -> {
            if (kafkaAktivitetWrapperDTO instanceof KafkaTiltaksAktivitet aktivitet) {
                aktivitetskortService.upsertAktivitetskort(aktivitet.payload);
            } else {
                throw new NotImplementedException("Unknown kafka message");
            }
            return ConsumeStatus.OK;
        }, kafkaAktivitetWrapperDTO);
    }

    private ConsumeStatus consumeWithFeilhandtering(Supplier<ConsumeStatus> block, KafkaAktivitetWrapperDTO message) {
        try {
            MDC.put(MetricService.SOURCE, message.source);
            return block.get();
        } /*catch (AktivitetsKortFunksjonellException e) {
            feilProducer.publishAktivitetsFeil(e,  message.messageId,  message.funksjonellId());
        } */finally {
            MDC.clear();
            return ConsumeStatus.OK;
        }
    }
}
