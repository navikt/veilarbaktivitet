package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.common.kafka.consumer.ConsumeStatus;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AktivitetskortConsumer extends TopicConsumerConfig<String, AktivitetskortDTO> implements TopicConsumer<String, AktivitetskortDTO> {

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
    public ConsumeStatus consume(ConsumerRecord<String, AktivitetskortDTO> record) {
        AktivitetskortDTO aktivitetskortDTO = record.value();
        ActionType type = aktivitetskortDTO.actionType();

        switch (type){
            case UPSERT_TILTAK_AKTIVITET_V1 -> {
                aktivitetskortService.opprettTiltaksaktivitet(aktivitetskortDTO);
            }
            case UPSERT_UTDANNING_AKTIVITET_V1 -> {
            }
            case UPSERT_GRUPPE_AKTIVITET_V1 -> {
            }
        }

        return null;
    }
}
