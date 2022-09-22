package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.veilarbaktivitet.config.kafka.AivenConsumerConfig;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AktivitetskortConsumer extends AivenConsumerConfig<String, AktivitetskortDTO> implements TopicConsumer<String, AktivitetskortDTO> {

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
        this.setValueDeserializer(Deserializers.jsonDeserializer(AktivitetskortDTO.class));
        this.setConsumer(this);
    }

    @Override
    public ConsumeStatus consume(ConsumerRecord<String, AktivitetskortDTO> consumerRecord) {
        AktivitetskortDTO aktivitetskortDTO = consumerRecord.value();
        ActionType actionType = aktivitetskortDTO.actionType;

        switch (actionType) {
            case UPSERT_TILTAK_AKTIVITET_V1 ->
                    aktivitetskortService.upsertAktivitetskort(aktivitetskortDTO);
            case UPSERT_GRUPPE_AKTIVITET_V1, UPSERT_UTDANNING_AKTIVITET_V1 -> throw new NotImplementedException();
        }

        return ConsumeStatus.OK;
    }
}
