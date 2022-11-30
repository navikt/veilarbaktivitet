package no.nav.veilarbaktivitet.kvp;

import no.nav.common.kafka.consumer.util.TopicConsumerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class KvpAvsluttetConsumerConfig extends TopicConsumerConfig<String, KvpAvsluttetKafkaDTO> {

    public KvpAvsluttetConsumerConfig(
            KvpAvsluttetKafkaConsumer consumer,
            @Value("${topic.inn.kvpAvsluttet}")
                    String topic
    ) {
        super();
        this.setTopic(topic);
        this.setKeyDeserializer(Deserializers.stringDeserializer());
        this.setValueDeserializer(Deserializers.jsonDeserializer(KvpAvsluttetKafkaDTO.class));
        this.setConsumer(consumer);
    }
}
