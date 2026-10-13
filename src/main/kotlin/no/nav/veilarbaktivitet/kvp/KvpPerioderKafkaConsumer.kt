package no.nav.veilarbaktivitet.kvp

import io.micrometer.core.annotation.Timed
import no.nav.common.kafka.consumer.ConsumeStatus
import no.nav.common.kafka.consumer.TopicConsumer
import no.nav.veilarbaktivitet.person.Person
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

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

                if (melding.avsluttet?.avsluttetDato != null && melding.avsluttet.avsluttetDato
                    .isBefore(ZonedDateTime.of(2026, 10, 13, 0, 0, 0, 0, ZoneId.of("Europe/Oslo")))
                ) {
                    return ConsumeStatus.OK
                }

                val begrunnelse = Optional.ofNullable(melding.avsluttet?.avsluttetBegrunnelse)

                kvpAvsluttetService.håndterKvpAvsluttet(aktorId, begrunnelse, sluttDato)
                return ConsumeStatus.OK
            }
        }
    }
}
