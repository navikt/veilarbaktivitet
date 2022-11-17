package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.veilarbaktivitet.aktivitetskort.feil.AktivitetsKortFunksjonellException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AktivitetsKortFeilProducer {

    @Autowired
    KafkaProducerClient<String, String> aivenProducerClient;

    @Value("${topic.ut.aktivitetskort-feil}")
    String feiltopic;

    @Autowired
    AktivitetskortMetrikker aktivitetskortMetrikker;

    private void publishAktivitetsFeil(AktivitetskortFeilMelding melding) {
        var producerRecord = new ProducerRecord<String, String>(feiltopic, melding.key(), JsonUtils.toJson(melding));
        aivenProducerClient.sendSync(producerRecord);
    }

    public void publishAktivitetsFeil(AktivitetsKortFunksjonellException e, ConsumerRecord<String, String> consumerRecord) {
        publishAktivitetsFeil(AktivitetskortFeilMelding.builder()
                .key(consumerRecord.key())
                .failingMessage(consumerRecord.value())
                .errorMessage(String.format("%s %s", e.getClass(), e.getMessage()))
                .timestamp(LocalDateTime.now())
                .build()
        );

        aktivitetskortMetrikker.countAktivitetskortFunksjonellFeil(e.getClass().getName());
    }
}
