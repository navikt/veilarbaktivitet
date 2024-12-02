package no.nav.fo.veilarbdialog.oversiktenVaas

import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
open class OversiktenProducer(
    val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${application.topic.ut.oversikten}")
    private val topic: String,
) {

    open fun sendMelding(key: String, melding: String) {
        kafkaTemplate.send(topic, key, melding)
    }
}