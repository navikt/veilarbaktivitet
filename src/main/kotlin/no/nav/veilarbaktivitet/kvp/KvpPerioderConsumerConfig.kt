package no.nav.veilarbaktivitet.kvp

import no.nav.common.kafka.consumer.util.TopicConsumerConfig
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class KvpPerioderConsumerConfig(
    consumer: KvpPerioderKafkaConsumer,
    @Value("\${topic.inn.kvpPerioder}")
    topic: String
) : TopicConsumerConfig<String, KvpPerioderKafkaDTO>() {
    init {
        this.setTopic(topic)
        this.setKeyDeserializer(Deserializers.stringDeserializer())
        this.setValueDeserializer(Deserializers.jsonDeserializer(KvpPerioderKafkaDTO::class.java))
        this.setConsumer(consumer)
    }
}
