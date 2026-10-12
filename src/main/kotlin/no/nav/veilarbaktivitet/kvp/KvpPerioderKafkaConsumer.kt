package no.nav.veilarbaktivitet.kvp

import io.micrometer.core.annotation.Timed
import no.nav.common.kafka.consumer.ConsumeStatus
import no.nav.common.kafka.consumer.TopicConsumer
import no.nav.common.kafka.consumer.util.TopicConsumerConfig
import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import no.nav.veilarbaktivitet.person.Person
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class KvpPerioderKafkaConsumer(
    private val kvpAvsluttetService: KVPAvsluttetService,
    @Value("\${topic.inn.kvpPerioder}")
    topic: String
) : TopicConsumerConfig<String, KvpPerioderKafkaDTO>(), TopicConsumer<String, KvpPerioderKafkaDTO> {
    companion object {
        private const val STANDARD_AVSLUTTET_BEGRUNNELSE = "KVP avsluttet"
    }

    init {
        this.setTopic(topic)
        this.setKeyDeserializer(Deserializers.stringDeserializer())
        this.setValueDeserializer(Deserializers.jsonDeserializer(KvpPerioderKafkaDTO::class.java))
        this.setConsumer(this)
    }

    @Timed(value = "kvp_perioder_consumer")
    override fun consume(consumerRecord: ConsumerRecord<String, KvpPerioderKafkaDTO>): ConsumeStatus {
        val melding = consumerRecord.value()
        val aktorId = Person.aktorId(melding.aktorId)

        when (melding.event) {
            KvpPeriodeEvent.STARTET -> return ConsumeStatus.OK
            KvpPeriodeEvent.AVSLUTTET -> {
                val sluttDato = melding.hentSluttTidspunkt()
                    .map { zonedDateTime -> Date.from(zonedDateTime.toInstant()) }
                    .orElseThrow { IllegalArgumentException("Mangler sluttDato/avsluttet for KVP AVSLUTTET") }

                val kontorsperreEnhetId = melding.enhetId
                val finnesAktiveAktiviteterMedKontorsperre = kvpAvsluttetService.finnesAktivitetMedKontorsperreEnhet(aktorId, kontorsperreEnhetId)
                if (kontorsperreEnhetId != null && !finnesAktiveAktiviteterMedKontorsperre) {
                    return ConsumeStatus.OK
                }

                kvpAvsluttetService.settAktiviteterInomKVPPeriodeTilAvbrutt(aktorId, STANDARD_AVSLUTTET_BEGRUNNELSE, sluttDato)
                return ConsumeStatus.OK
            }
            null -> throw IllegalArgumentException("Ukjent KVP event: ${melding.event}")
        }
    }
}
