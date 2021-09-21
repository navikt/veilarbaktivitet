package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avro.*;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.InnsenderData;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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

    void sendIkkeOpprettet(AktivitetData aktivitetData) {
        sendRespons(TilstandEnum.KAN_IKKE_OPPRETTE, aktivitetData);
    }

    void sendOpprettetIkkeVarslet(AktivitetData aktivitetData) {
        sendRespons(TilstandEnum.KAN_IKKE_VARSLE, aktivitetData);
    }

    void sendOpprettet(AktivitetData aktivitetData) {
        sendRespons(TilstandEnum.PROVER_VARSLING, aktivitetData);
    }

    void sendVarslet(AktivitetData aktivitetData) {
        sendRespons(TilstandEnum.HAR_VARSLET, aktivitetData);
    }

    void sendSvart(AktivitetData aktivitetData) {
        sendRespons(TilstandEnum.HAR_SVART, aktivitetData);
    }

    void sendSvarfristUtlopt(AktivitetData aktivitetData) {
        sendRespons(TilstandEnum.SVARFRIST_UTLOPT, aktivitetData);
    }

    private void sendRespons(TilstandEnum tilstand, AktivitetData aktivitetData) {
        DelingAvCvRespons delingAvCvRespons = new DelingAvCvRespons();
        delingAvCvRespons.setBestillingsId(aktivitetData.getStillingFraNavData().getBestillingsId());
        delingAvCvRespons.setAktorId(aktivitetData.getAktorId());
        delingAvCvRespons.setAktivitetId(aktivitetData.getId() != null ? aktivitetData.getId().toString() : null);
        delingAvCvRespons.setTilstand(tilstand);

        delingAvCvRespons.setSvar(getSvar(aktivitetData.getStillingFraNavData().getCvKanDelesData()));

        ProducerRecord<String, DelingAvCvRespons> stringDelingAvCvResponsProducerRecord = new ProducerRecord<>(topicUt, delingAvCvRespons.getBestillingsId(), delingAvCvRespons);
        log.info("StillingFraNavProducerClient.sendRespons:{}", stringDelingAvCvResponsProducerRecord);
        producerClient.send(stringDelingAvCvResponsProducerRecord);
    }

    private Svar getSvar(CvKanDelesData cvKanDelesData) {
        if(cvKanDelesData == null) {
            return null;
        } else {
            Svar svar = new Svar();
            svar.setSvar(cvKanDelesData.kanDeles);
            Ident ident = new Ident();
            ident.setIdent(cvKanDelesData.endretAv);
            svar.setSvarTidspunkt(cvKanDelesData.getEndretTidspunkt().toInstant());
            if(cvKanDelesData.getEndretAvType() == InnsenderData.NAV) {
                ident.setIdentType(IdentTypeEnum.NAV_IDENT);
            } else {
                ident.setIdentType(IdentTypeEnum.AKTOR_ID);
            }
            svar.setSvartAv(ident);
            return svar;
        }
    }


}
