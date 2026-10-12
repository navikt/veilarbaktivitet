package no.nav.veilarbaktivitet.kvp

import io.micrometer.core.annotation.Timed
import no.nav.common.kafka.consumer.ConsumeStatus
import no.nav.common.kafka.consumer.TopicConsumer
import no.nav.veilarbaktivitet.person.Person
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service
import java.util.Date
import java.util.Optional

@Service
class KvpPerioderKafkaConsumer(
    private val kvpAvsluttetService: KVPAvsluttetService
) : TopicConsumer<String, KvpPerioderKafkaDTO> {
    @Timed(value = "kvp_perioder_consumer")
    override fun consume(consumerRecord: ConsumerRecord<String, KvpPerioderKafkaDTO>): ConsumeStatus {
        val melding = consumerRecord.value()
        val aktorId = Person.aktorId(melding.aktorId)

        when (melding.event) {
            KvpPeriodeEventType.STARTET -> return ConsumeStatus.OK
            KvpPeriodeEventType.AVSLUTTET -> {
                val sluttDato = Optional.ofNullable(melding.avsluttet?.avsluttetDato)
                    .map { zonedDateTime -> Date.from(zonedDateTime.toInstant()) }
                val begrunnelse = Optional.ofNullable(melding.avsluttet?.avsluttetBegrunnelse)

                kvpAvsluttetService.håndterKvpAvsluttet(aktorId, begrunnelse, sluttDato)
                return ConsumeStatus.OK
            }
        }
    }
}
