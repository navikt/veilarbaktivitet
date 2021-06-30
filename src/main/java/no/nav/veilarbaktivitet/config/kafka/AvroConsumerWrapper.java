package no.nav.veilarbaktivitet.config.kafka;

import lombok.Value;
import no.nav.common.kafka.consumer.util.JsonTopicConsumer;

import java.util.function.Consumer;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.jsonConsumer;

@Value
public class AvroConsumerWrapper<T> {
    public AvroConsumerWrapper(String topic, Class<T> klasse, Consumer<T> consumer ) {
        this.consumer = jsonConsumer(klasse, consumer);
        this.topic = topic;
    }

    String topic;
    JsonTopicConsumer<String, String, T> consumer;
}
