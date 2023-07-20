package no.nav.veilarbaktivitet.aktivitetskort

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.common.json.JsonUtils
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMapping
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDto
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
/**
 * Publiserer koblingen mellom teknisk id, arena-id og funksjonell id.
 * Denne brukes i dialog for å gjøre løpende migrering fra arena-id til teknisk id på gamle dialoger på arena-aktiviteter.
 */
class AktivitetIdMappingProducer {
    @Autowired
    var aivenProducerClient: KafkaProducerClient<String, String>? = null

    @Value("\${topic.ut.aktivitetskort-idmapping}")
    var idmappingTopic: String? = null

    init {
        JsonUtils.getMapper().registerKotlinModule()
    }
    fun publishAktivitetskortIdMapping(idMapping: IdMapping) {
        val melding = IdMappingDto.map(idMapping)
        val producerRecord = ProducerRecord(idmappingTopic, melding.funksjonellId.toString(), JsonUtils.toJson(melding))
        aivenProducerClient!!.sendSync(producerRecord)
    }
}
