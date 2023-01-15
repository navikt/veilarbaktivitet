package no.nav.veilarbaktivitet.aktivitetskort.test;

import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AktivitetsKortTestConsumerConfig extends TopicConsumerConfig<String, String> {
    public AktivitetsKortTestConsumerConfig(
            @Value("${topic.inn.aktivitetskort}")
            String topic,
            AktivitetskortTestConsumer consumer
    ) {
        this.setTopic(topic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(Deserializers.stringDeserializer());
        this.setConsumer(consumer);
    }
}

