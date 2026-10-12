package no.nav.veilarbaktivitet.kvp

import io.micrometer.core.annotation.Timed
import no.nav.common.kafka.consumer.ConsumeStatus
import no.nav.common.kafka.consumer.TopicConsumer
import no.nav.veilarbaktivitet.person.Person
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service
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
            KvpPeriodeEvent.STARTET -> return ConsumeStatus.OK
            KvpPeriodeEvent.AVSLUTTET -> {
                val sluttDato = melding.hentSluttTidspunkt().map { zonedDateTime -> Date.from(zonedDateTime.toInstant()) }
                    .orElseThrow { IllegalArgumentException("Mangler sluttDato/avsluttet for KVP AVSLUTTET") }

                kvpAvsluttetService.håndterKvpAvsluttet(aktorId, Optional.empty(), Optional.of(sluttDato))
                return ConsumeStatus.OK
            }
            null -> throw IllegalArgumentException("Ukjent KVP event: ${melding.event}")
        }
    }
}
