package no.nav.veilarbaktivitet.stilling_fra_nav;

import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.ForesporselOmDelingAvCv;
import no.nav.veilarbaktivitet.avro.SvarEnum;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class StillingFraNavProducerClient {
    private final KafkaTemplate<String, DelingAvCvRespons> producerClient;
    private final String topicUt;

    public StillingFraNavProducerClient(
            KafkaTemplate<String, DelingAvCvRespons> producerClient,
            @Value("${topic.ut.stillingFraNav}") String topicUt
    ) {
        this.producerClient = producerClient;
        this.topicUt = topicUt;
    }

    void sendIkkeOpprettet(AktivitetData aktivitetData, ForesporselOmDelingAvCv melding) {
        sendRespons(false, false, melding, aktivitetData);
    }

    void sendOpprettetIkkeVarslet(AktivitetData aktivitetData, ForesporselOmDelingAvCv melding) {
        sendRespons(true, false, melding, aktivitetData);
    }

    void sendOpprettet(AktivitetData aktivitetData, ForesporselOmDelingAvCv melding) {
        sendRespons(true, true, melding, aktivitetData);
    }

    private void sendRespons(boolean aktivitetOpprettet, boolean brukerVarslet, ForesporselOmDelingAvCv melding, AktivitetData aktivitetData) {
        DelingAvCvRespons delingAvCvRespons = new DelingAvCvRespons();
        delingAvCvRespons.setBestillingsId(melding.getBestillingsId());
        delingAvCvRespons.setAktivitetOpprettet(aktivitetOpprettet);
        delingAvCvRespons.setBrukerVarslet(brukerVarslet);
        delingAvCvRespons.setAktorId(melding.getAktorId());
        delingAvCvRespons.setAktivitetId(aktivitetData.getId() != null ? aktivitetData.getId().toString() : null);
        delingAvCvRespons.setBrukerSvar(SvarEnum.IKKE_SVART);


        ProducerRecord<String, DelingAvCvRespons> stringDelingAvCvResponsProducerRecord = new ProducerRecord<>(topicUt, delingAvCvRespons.getBestillingsId(), delingAvCvRespons);
        producerClient.send(stringDelingAvCvResponsProducerRecord);
    }
}
