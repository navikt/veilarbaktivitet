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

    void sendUgyldigOppfolgingStatus(String bestillingsId, String aktorId) {
        KanIkkeOppretteBegrunnelse kanIkkeOppretteBegrunnelse = KanIkkeOppretteBegrunnelse.newBuilder()
                .setBegrunnelse(BegrunnelseEnum.UGYLDIG_OPPFOLGINGSSTATUS)
                .setFeilmelding(null)
                .build();
        sendRespons(TilstandEnum.KAN_IKKE_OPPRETTE,
                bestillingsId,
                aktorId,
                null,
                null,
                kanIkkeOppretteBegrunnelse);
    }

    void sendUgyldigInput(String bestillingsId, String aktorId, String feilmelding) {
        sendRespons(TilstandEnum.KAN_IKKE_OPPRETTE,
                bestillingsId,
                aktorId,
                null,
                null,
                KanIkkeOppretteBegrunnelse.newBuilder().setBegrunnelse(BegrunnelseEnum.UGYLDIG_INPUT).setFeilmelding(feilmelding).build());
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
        sendRespons(tilstand,
                aktivitetData.getStillingFraNavData().getBestillingsId(),
                aktivitetData.getAktorId(),
                aktivitetData.getId()
                , getSvar(aktivitetData.getStillingFraNavData().getCvKanDelesData())
                , null);
    }

    private void sendRespons(TilstandEnum tilstand,
                             String bestillingsId,
                             String aktorId,
                             Long aktivitetId,
                             Svar svar,
                             KanIkkeOppretteBegrunnelse kanIkkeOppretteBegrunnelse) {
        if (TilstandEnum.KAN_IKKE_OPPRETTE.equals(tilstand) && kanIkkeOppretteBegrunnelse == null) {
            throw new IllegalArgumentException("KanIkkeOppretteBegrunnelse er påkrevd ved KAN_IKKE_OPPRETTE");
        }
        DelingAvCvRespons delingAvCvRespons = new DelingAvCvRespons();
        delingAvCvRespons.setBestillingsId(bestillingsId);
        delingAvCvRespons.setAktorId(aktorId);
        delingAvCvRespons.setAktivitetId(aktivitetId != null ? aktivitetId.toString() : null);
        delingAvCvRespons.setTilstand(tilstand);

        delingAvCvRespons.setSvar(svar);

        delingAvCvRespons.setKanIkkeOppretteBegrunnelse(kanIkkeOppretteBegrunnelse);

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
