package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.veilarbaktivitet.config.kafka.AivenConsumerConfig;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@ToString(of = {"aktivitetskortService"})
public class AktivitetskortConsumer extends AivenConsumerConfig<String, KafkaAktivitetWrapperDTO> implements TopicConsumer<String, KafkaAktivitetWrapperDTO> {

    private final AktivitetskortService aktivitetskortService;

    public AktivitetskortConsumer(
            AktivitetskortService aktivitetskortService,
            @Value("${topic.inn.aktivitetskort}")
            String topic
    ) {
        super();
        this.aktivitetskortService = aktivitetskortService;
        this.setTopic(topic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(Deserializers.jsonDeserializer(KafkaAktivitetWrapperDTO.class));
        this.setConsumer(this);
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, KafkaAktivitetWrapperDTO> consumerRecord) {
        KafkaAktivitetWrapperDTO kafkaAktivitetWrapperDTO = consumerRecord.value();

        if (hasSeenMessage(kafkaAktivitetWrapperDTO.messageId)) {
            log.warn("Previously handled message seen {} , ignoring", kafkaAktivitetWrapperDTO.messageId);
            return ConsumeStatus.OK;
        }

        if (kafkaAktivitetWrapperDTO instanceof KafkaTiltaksAktivitet aktivitet) {
            aktivitetskortService.upsertAktivitetskort(aktivitet.payload);
        } else {
            throw new NotImplementedException("Unknown kafka message");
        }

        return ConsumeStatus.OK;
    }

    private boolean hasSeenMessage(UUID messageId) {
        // TODO: Implement
        return false;
    }
}
