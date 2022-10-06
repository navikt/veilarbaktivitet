package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.veilarbaktivitet.config.kafka.AivenConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AktivitetsKortConsumerConfig extends AivenConsumerConfig<String, String> {
    public AktivitetsKortConsumerConfig(
            @Value("${topic.inn.aktivitetskort}")
            String topic,
            AktivitetskortConsumer consumer
    ) {
        this.setTopic(topic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(Deserializers.stringDeserializer());
        this.setConsumer(consumer);
    }
}

