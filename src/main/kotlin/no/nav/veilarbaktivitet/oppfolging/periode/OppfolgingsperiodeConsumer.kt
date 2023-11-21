package no.nav.veilarbaktivitet.oppfolging.periode

import no.nav.common.json.JsonUtils
import no.nav.common.kafka.consumer.ConsumeStatus
import no.nav.common.kafka.consumer.TopicConsumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class OppfolgingsperiodeConsumer(
    private val oppfolgingsperiodeService: OppfolgingsperiodeService
) : TopicConsumer<String, String> {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun consume(consumerRecord: ConsumerRecord<String?, String?>): ConsumeStatus {
        val sisteOppfolgingsperiodeV1 = JsonUtils.fromJson(
            consumerRecord.value(),
            SisteOppfolgingsperiodeV1::class.java
        )
        log.info("oppfølgingsperiode: {}", sisteOppfolgingsperiodeV1)
        oppfolgingsperiodeService.upsertOppfolgingsperiode(sisteOppfolgingsperiodeV1)
        if (sisteOppfolgingsperiodeV1.sluttDato != null) {
            oppfolgingsperiodeService.avsluttOppfolgingsperiode(
                sisteOppfolgingsperiodeV1.uuid,
                sisteOppfolgingsperiodeV1.sluttDato
            )
        }
        return ConsumeStatus.OK
    }
}
