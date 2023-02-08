package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AktivitetsKortConsumerConfig extends TopicConsumerConfig<String, String> {

    public AktivitetsKortConsumerConfig(
            @Value("${topic.inn.aktivitetskort}") String topic,
            AktivitetskortConsumer consumer
    ) {
        this.setTopic(topic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(Deserializers.stringDeserializer());
        this.setConsumer(consumer);
    }
}

