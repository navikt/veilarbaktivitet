package no.nav.veilarbaktivitet.stilling_fra_nav;

import no.nav.veilarbaktivitet.avro.DelingAvCvRespons;
import no.nav.veilarbaktivitet.avro.TilstandEnum;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.ForesporselOmDelingAvCv;
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
        sendRespons(TilstandEnum.IKKE_OPPRETTET, melding, aktivitetData);
    }

    void sendOpprettetIkkeVarslet(AktivitetData aktivitetData, ForesporselOmDelingAvCv melding) {
        sendRespons(TilstandEnum.IKKE_VARSLET, melding, aktivitetData);
    }

    void sendOpprettet(AktivitetData aktivitetData, ForesporselOmDelingAvCv melding) {
        sendRespons(TilstandEnum.PROVER_VARSLING, melding, aktivitetData);
    }

    private void sendRespons(TilstandEnum tilstand, ForesporselOmDelingAvCv melding, AktivitetData aktivitetData) {
        DelingAvCvRespons delingAvCvRespons = new DelingAvCvRespons();
        delingAvCvRespons.setBestillingsId(melding.getBestillingsId());
        delingAvCvRespons.setAktorId(melding.getAktorId());
        delingAvCvRespons.setAktivitetId(aktivitetData.getId() != null ? aktivitetData.getId().toString() : null);
        delingAvCvRespons.setTilstand(tilstand);
        delingAvCvRespons.setSvar(null);


        ProducerRecord<String, DelingAvCvRespons> stringDelingAvCvResponsProducerRecord = new ProducerRecord<>(topicUt, delingAvCvRespons.getBestillingsId(), delingAvCvRespons);
        producerClient.send(stringDelingAvCvResponsProducerRecord);
    }
}
