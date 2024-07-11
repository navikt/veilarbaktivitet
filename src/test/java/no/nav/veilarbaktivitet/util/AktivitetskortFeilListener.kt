package no.nav.veilarbaktivitet.util

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@Service
class AktivitetskortFeilListener {

    var record: ConsumerRecord<String, String>? = null
    val blockingQueue: BlockingQueue<ConsumerRecord<String, String>> = LinkedBlockingQueue()

    @KafkaListener(id = "aktivitetskortFeil", topics = ["\${topic.ut.aktivitetskort-feil}"], containerFactory = "stringStringKafkaListenerContainerFactory")
    fun listen(record: ConsumerRecord<String, String>?) {
        this.record = record
        blockingQueue.add(record!!)
    }

    fun getSingleRecord(): ConsumerRecord<String, String>? {
        return blockingQueue.poll(30, TimeUnit.SECONDS)
    }

    fun take(number: Int = 1): List<ConsumerRecord<String, String>> {
        return blockingQueue.take(number)
    }

    fun clearQueue() {
        blockingQueue.clear()
    }
}