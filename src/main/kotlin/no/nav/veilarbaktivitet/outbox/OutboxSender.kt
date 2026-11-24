package no.nav.veilarbaktivitet.outbox

import org.slf4j.LoggerFactory
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class OutboxSender(
    private val outboxDao: OutboxDao,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(OutboxSender::class.java)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun sendMeldinger(event: OutboxService.OutboxMeldingLagretEvent? = null) {
        try {
            val meldinger = outboxDao.hentUsendteMeldinger(event)
            meldinger.forEach { melding ->
                kafkaTemplate.send(melding.topic, melding.key, melding.payload).get()
                outboxDao.slett(melding.id)
                log.info("Outbox-melding sendt og slettet: id={}, topic={}", melding.id, melding.topic)
            }
        } catch (e: PessimisticLockingFailureException) {
                log.warn("LOL")
        }
    }
}