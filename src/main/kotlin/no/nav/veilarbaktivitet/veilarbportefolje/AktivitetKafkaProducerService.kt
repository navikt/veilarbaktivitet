package no.nav.veilarbaktivitet.veilarbportefolje

import io.micrometer.core.annotation.Timed
import lombok.RequiredArgsConstructor
import lombok.SneakyThrows
import lombok.extern.slf4j.Slf4j
import no.nav.common.json.JsonUtils
import no.nav.common.rest.filter.LogRequestFilter
import no.nav.veilarbaktivitet.config.kafka.kafkatemplates.KafkaStringTemplate
import no.nav.veilarbaktivitet.veilarbportefolje.dto.KafkaAktivitetMeldingV4
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.header.internals.RecordHeaders
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Slf4j
@RequiredArgsConstructor
@Service
class AktivitetKafkaProducerService(
    private val portefoljeProducer: KafkaStringTemplate,
    private val dao: KafkaAktivitetDAO,
    @Value("\${topic.ut.portefolje}")
    private val portefoljeTopic: String
) {

    @Timed("aktivitet_til_kafka")
    @SneakyThrows
    fun sendAktivitetMelding(melding: KafkaAktivitetMeldingV4) {
        val portefoljeMelding = ProducerRecord(
            portefoljeTopic,
            null,
            melding.aktorId,
            JsonUtils.toJson(melding),
            RecordHeaders().add(
                RecordHeader(LogRequestFilter.NAV_CALL_ID_HEADER_NAME, correlationId.toByteArray())
            )
        )

        val offset = portefoljeProducer.send(portefoljeMelding).get().recordMetadata.offset()

        dao.updateSendtPaKafkaAven(melding.version, offset)
    }

    companion object {
        val correlationId: String
            get() {
                return MDC.get(LogRequestFilter.NAV_CALL_ID_HEADER_NAME)
                    ?: MDC.get("jobId")
                    ?: UUID.randomUUID().toString()
            }
    }
}
