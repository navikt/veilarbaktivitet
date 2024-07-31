package no.nav.veilarbaktivitet.util

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Service
class AktivitetskortIdMappingListener {

    var record: ConsumerRecord<String, String>? = null
    var countDownLatch = CountDownLatch(1)

    @KafkaListener(id = "aktivitetskortId", topics = ["\${topic.ut.aktivitetskort-idmapping}"], containerFactory = "stringStringKafkaListenerContainerFactory")
    fun listen(record: ConsumerRecord<String, String>?) {
        this.record = record
        countDownLatch.countDown()
    }

    fun getSingleRecord(): ConsumerRecord<String, String>? {
        val success = countDownLatch.await(3, TimeUnit.SECONDS)
        if (!success) throw RuntimeException("Timout when waiting for message on topic")
        return record
    }

}