package no.nav.veilarbaktivitet.oversikten

import no.nav.common.kafka.producer.KafkaProducerClient
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
open class OversiktenProducer(
    @Autowired
    val aivenProducerClient: KafkaProducerClient<String, String>,
    @Value("\${topic.ut.oversikten}")
    private val topic: String,
) {

    open fun sendMelding(key: String, melding: String) {
        val producerRecord = ProducerRecord(topic, key, melding)
        aivenProducerClient.sendSync(producerRecord)
    }
}