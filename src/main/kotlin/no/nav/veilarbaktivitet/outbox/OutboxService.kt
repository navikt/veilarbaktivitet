package no.nav.veilarbaktivitet.outbox

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener



@Service
class OutboxService(
    private val outboxDao: OutboxDao,
    private val eventPublisher: ApplicationEventPublisher,
    private val outboxSender: OutboxSender,
) {
    private val log = LoggerFactory.getLogger(OutboxService::class.java)

    @Transactional(propagation = Propagation.MANDATORY)
    fun lagreIOutbox(topic: String, key: String, payload: String?) {
        outboxDao.lagre(topic, key, payload)
        eventPublisher.publishEvent(OutboxMeldingLagretEvent(key = key, topic = topic))
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun sendEtterCommit(event: OutboxMeldingLagretEvent) {
        try {
            outboxSender.sendMeldinger(event)
        } catch (e: Exception) {
            log.warn("Kunne ikke sende outbox-meldinger etter commit, fallback-job vil forsøke på nytt", e)
        }
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    @SchedulerLock(name = "outbox_kafka_scheduledTask", lockAtMostFor = "PT2M")
    fun sendUsendteMeldinger() {
        outboxSender.sendMeldinger()
    }

    data class OutboxMeldingLagretEvent(
        val topic: String,
        val key: String
    )
}
