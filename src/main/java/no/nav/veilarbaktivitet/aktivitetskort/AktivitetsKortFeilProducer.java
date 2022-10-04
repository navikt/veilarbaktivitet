package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AktivitetsKortFeilProducer {

    @Autowired
    KafkaProducerClient<String, String> producer;

    @Value("${topic.ut.aktivitetskort-feil}")
    String feiltopic;

    public void publishAktivitetsFeil(AktivitetskortFeilMelding melding) {
        var record = new ProducerRecord(feiltopic, melding.aktivitetId().toString(), JsonUtils.toJson(melding));
        producer.sendSync(record);
    }

    public void publishAktivitetsFeil(AktivitetsKortFunksjonellException e, UUID messageId, UUID aktivitetId) {
        publishAktivitetsFeil(AktivitetskortFeilMelding.builder()
                .aktivitetId(aktivitetId)
                .messageId(messageId)
                .payload("Payload here")
                .errorMessage(String.format("%s %s", e.getClass().getName(), e.getMessage()))
                .build()
        );
    }
}
