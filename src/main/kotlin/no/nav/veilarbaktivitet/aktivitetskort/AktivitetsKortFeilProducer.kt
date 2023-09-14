package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.common.json.JsonUtils
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.AktivitetskortFeilMelding
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource
import no.nav.veilarbaktivitet.aktivitetskort.feil.AktivitetsKortFunksjonellException
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AktivitetsKortFeilProducer (
    @Autowired
    val aivenProducerClient: KafkaProducerClient<String, String>,
    @Autowired
    var aktivitetskortMetrikker: AktivitetskortMetrikker
) {

    @Value("\${topic.ut.aktivitetskort-feil}")
    var feiltopic: String? = null

    private fun publishAktivitetsFeil(melding: AktivitetskortFeilMelding) {
        val producerRecord = ProducerRecord(feiltopic, melding.key, JsonUtils.toJson(melding))
        aivenProducerClient.sendSync(producerRecord)
    }

    fun publishAktivitetsFeil(error: AktivitetsKortFunksjonellException, consumerRecord: ConsumerRecord<String, String>, source: MessageSource) {
        publishAktivitetsFeil(
            AktivitetskortFeilMelding(
                consumerRecord.key(),
                LocalDateTime.now(),
                consumerRecord.value(),
                String.format("%s %s", error.javaClass, error.message),
                source,
                ErrorType.of(error)
            )
        )
        aktivitetskortMetrikker.countAktivitetskortFunksjonellFeil(error.javaClass.name, source)
    }
}
