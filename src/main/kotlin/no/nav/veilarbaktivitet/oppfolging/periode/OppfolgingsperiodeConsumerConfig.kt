package no.nav.veilarbaktivitet.oppfolging.periode

import no.nav.common.kafka.consumer.util.TopicConsumerConfig
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
open class OppfolgingsperiodeConsumerConfig(
    @Value("\${topic.inn.oppfolgingsperiode}") topic: String,
    consumer: OppfolgingsperiodeConsumer
) : TopicConsumerConfig<String, String>(
    topic,
    Deserializers.stringDeserializer(),
    Deserializers.stringDeserializer(),
    consumer
)
