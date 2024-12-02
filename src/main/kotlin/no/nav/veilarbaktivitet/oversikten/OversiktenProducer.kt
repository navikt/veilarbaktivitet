package no.nav.veilarbaktivitet.oversikten

//
//@Service
//open class OversiktenProducer(
//    val kafkaTemplate: KafkaTemplate<String, String>,
//    @Value("\${application.topic.ut.oversikten}")
//    private val topic: String,
//) {
//
//    open fun sendMelding(key: String, melding: String) {
//        kafkaTemplate.send(topic, key, melding)
//    }
//}