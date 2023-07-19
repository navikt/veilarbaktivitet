package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.common.kafka.consumer.util.TopicConsumerConfig
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AktivitetsKortConsumerConfig(
    @Value("\${topic.inn.aktivitetskort}") topic: String,
    consumer: AktivitetskortConsumer
) : TopicConsumerConfig<String, String>(
    topic,
    Deserializers.stringDeserializer(),
    Deserializers.stringDeserializer(),
    consumer
)
