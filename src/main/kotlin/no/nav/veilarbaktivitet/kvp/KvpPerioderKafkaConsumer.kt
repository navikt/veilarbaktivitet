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
import java.util.Date

@Service
class KvpPerioderKafkaConsumer(
    private val kvpAvsluttetService: KVPAvsluttetService,
    private val dedupService: KvpHendelseDedupService,
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

        if (melding.event == KvpPeriodeEvent.STARTET) {
            val startetDato = melding.hentStartTidspunkt()
                .map { zonedDateTime -> Date.from(zonedDateTime.toInstant()) }
                .orElseThrow { IllegalArgumentException("Mangler startDato/startet for KVP STARTET") }

            dedupService.reserverStartetHendelse(aktorId, startetDato)
            return ConsumeStatus.OK
        }

        if (melding.event == KvpPeriodeEvent.AVSLUTTET) {
            val sluttDato = melding.hentSluttTidspunkt()
                .map { zonedDateTime -> Date.from(zonedDateTime.toInstant()) }
                .orElseThrow { IllegalArgumentException("Mangler sluttDato/avsluttet for KVP AVSLUTTET") }

            if (!dedupService.reserverAvsluttetHendelse(aktorId, sluttDato)) {
                return ConsumeStatus.OK
            }

            kvpAvsluttetService.settAktiviteterInomKVPPeriodeTilAvbrutt(aktorId, STANDARD_AVSLUTTET_BEGRUNNELSE, sluttDato)
            return ConsumeStatus.OK
        }

        throw IllegalArgumentException("Ukjent KVP event: ${melding.event}")
    }
}
