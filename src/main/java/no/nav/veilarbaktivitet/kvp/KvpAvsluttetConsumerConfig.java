package no.nav.veilarbaktivitet.kvp;

import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class KvpAvsluttetConsumerConfig extends TopicConsumerConfig<String, KvpAvsluttetDTO> {

    public KvpAvsluttetConsumerConfig(
            @Value("${topic.inn.kvpAvsluttet}") String topic,
            KvpAvsluttetConsumer consumer
    ) {
        this.setTopic(topic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(Deserializers.jsonDeserializer(KvpAvsluttetDTO.class));
        this.setConsumer(consumer);
    }
}
