package no.nav.veilarbaktivitet.kvp

import no.nav.common.kafka.consumer.util.TopicConsumerConfig
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class KvpPerioderConsumerConfig(
    @Value("\${topic.inn.kvpPerioder}") topic: String,
    consumer: KvpPerioderKafkaConsumer
) : TopicConsumerConfig<String, KvpPerioderKafkaDTO>(
    topic,
    Deserializers.stringDeserializer(),
    Deserializers.jsonDeserializer(KvpPerioderKafkaDTO::class.java),
    consumer
)
