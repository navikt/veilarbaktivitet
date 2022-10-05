package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AktivitetsKortFeilProducer {

    @Autowired
    KafkaProducerClient<String, String> producer;

    @Value("${topic.ut.aktivitetskort-feil}")
    String feiltopic;

    private void publishAktivitetsFeil(AktivitetskortFeilMelding melding) {
        var record = new ProducerRecord<String, String>(feiltopic, melding.key(), JsonUtils.toJson(melding));
        producer.sendSync(record);
    }

    public void publishAktivitetsFeil(AktivitetsKortFunksjonellException e) {
        publishAktivitetsFeil(AktivitetskortFeilMelding.builder()
                .key(e.key)
                .failingMessage(e.failingMessage.value())
                .errorMessage(String.format("%s %s", e.getClass(), e.getMessage()))
                .timestamp(LocalDateTime.now())
                .build()
        );
    }
}
