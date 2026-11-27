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
            meldinger.asSequence()
                .map { sendUtKafkaMelding(it) }
                .takeWhile { it.isSuccess }
                .count() // Må terminere sekvensen for å faktisk sende meldingene
        } catch (e: PessimisticLockingFailureException) {
                log.warn("Klarte ikke hente ut outbox meldinger, rader var låst")
        }
    }

    fun sendUtKafkaMelding(melding: OutboxRecord): Result<Unit> {
        return runCatching {
            kafkaTemplate.send(melding.topic, melding.key, melding.payload).get()
            outboxDao.slett(melding.id)
            log.info("Outbox-melding sendt og slettet: id={}, topic={}", melding.id, melding.topic)
        }
    }
}