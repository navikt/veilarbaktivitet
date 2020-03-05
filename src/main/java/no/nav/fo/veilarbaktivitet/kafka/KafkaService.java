package no.nav.fo.veilarbaktivitet.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.json.JsonUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static no.nav.fo.veilarbaktivitet.kafka.KafkaConfig.KAFKA_TOPIC_AKTIVITETER;

@Slf4j
@Component
public class KafkaService {
    private Producer<String, String> producer;
    private KafkaDAO database;

    @Inject
    public KafkaService(Producer<String, String> producer, KafkaDAO kafkaDAO) {
        this.producer = producer;
        this.database = kafkaDAO;
    }

    public void sendMelding(KafkaAktivitetMelding melding) {
        sjekkFeiledeMeldinger();
        send(melding);
    }

    private void sjekkFeiledeMeldinger() {
        Long antallFeiledeMeldinger = database.antallFeiledeMeldinger();

        if (antallFeiledeMeldinger > 0) {
            log.info("Fant {} feilede Kafka-meldinger, prøver å sende disse på nytt", antallFeiledeMeldinger);

            database.hentFeiledeMeldinger().forEach(feiletMelding -> {
                database.slett(feiletMelding);
                send(feiletMelding);
            });
        }
    }

    private void send(KafkaAktivitetMelding melding) {
        String key = melding.getAktorId();
        ProducerRecord<String, String> record = new ProducerRecord<>(KAFKA_TOPIC_AKTIVITETER, key, JsonUtils.toJson(melding));

        try {
            producer.send(record);
        } catch (Exception e) {
            log.error("Sending av aktivitet {} til kafka med correlationId {} for bruker med aktørId {} feilet", melding.getAktivitetId(), melding.getMeldingId(), melding.getAktorId());
            database.lagre(melding);
            return;
        }
        log.info("Sendte aktivitet {} på kafka med correlationId {} for bruker med aktørId {}", melding.getAktivitetId(), melding.getMeldingId(), melding.getAktorId());
    }
}
