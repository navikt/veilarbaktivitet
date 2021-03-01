package no.nav.veilarbaktivitet.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.job.JobRunner;
import no.nav.common.utils.IdUtils;
import no.nav.common.utils.fn.UnsafeRunnable;
import no.nav.veilarbaktivitet.kafka.dto.KvpAvsluttetKafkaDTO;
import no.nav.veilarbaktivitet.kafka.dto.OppfolgingAvsluttetKafkaDTO;
import no.nav.veilarbaktivitet.repository.KafkaRepository;
import no.nav.veilarbaktivitet.service.KafkaSevice;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static no.nav.common.json.JsonUtils.fromJson;

@Slf4j
@Component
public class KafkaConsumer {

    private final int BACKOFF_TIME_MS = 60_000;

    private final KafkaTopics kafkaTopics;

    private final KafkaSevice kafkaSevice;

    private final KafkaRepository kafkaRepository;

    @Autowired
    public KafkaConsumer(KafkaTopics kafkaTopics, KafkaSevice kafkaSevice, KafkaRepository kafkaRepository) {
        this.kafkaTopics = kafkaTopics;
        this.kafkaSevice = kafkaSevice;
        this.kafkaRepository = kafkaRepository;
    }

    @KafkaListener(topics = "#{kafkaTopics.getOppfolgingAvsluttet()}")
    public void konsumerOppfolgingAvsluttet(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        consumeWithErrorHandling(() -> {
            OppfolgingAvsluttetKafkaDTO oppfolgingAvsluttetDto = fromJson(record.value(), OppfolgingAvsluttetKafkaDTO.class);
            kafkaSevice.konsumerOppfolgingAvsluttet(oppfolgingAvsluttetDto);
        }, record, acknowledgment);
    }

    @KafkaListener(topics = "#{kafkaTopics.getKvpAvsluttet()}")
    public void konsumerKvpAvsluttet(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        consumeWithErrorHandling(() -> {
            KvpAvsluttetKafkaDTO kvpAvsluttetKafkaDTO = fromJson(record.value(), KvpAvsluttetKafkaDTO.class);
            kafkaSevice.konsumerKvpAvsluttet(kvpAvsluttetKafkaDTO);
        }, record, acknowledgment);
    }

    private void consumeWithErrorHandling(UnsafeRunnable runnable, ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String topic = record.topic();
        String key = record.key();
        String value = record.value();
        long offset = record.offset();

        String jobName = "konsumer_" + topic;
        String jobId = IdUtils.generateId();

        log.info("topic={} key={} offset={} jobId={} - Konsumerer melding fra topic", topic, key, offset, jobId);

        try {
            JobRunner.run(jobName, jobId, runnable::run);
            acknowledgment.acknowledge();
        } catch (Exception exception) {
            log.error(format("topic=%s key=%s offset=%d - Konsumering av melding feilet.", topic, key, offset), exception);

            try {
                kafkaRepository.lagreFeiletKonsumertKafkaMelding(kafkaTopics.strToTopic(topic), key, value, offset);
                acknowledgment.acknowledge();
            } catch (Exception e) {
                log.error(format("topic=%s key=%s offset=%d - Lagring av feilet melding feilet", topic, key, offset), exception);
                acknowledgment.nack(BACKOFF_TIME_MS);
            }
        }
    }

}
