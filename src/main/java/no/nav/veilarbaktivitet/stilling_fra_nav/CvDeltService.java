package no.nav.veilarbaktivitet.stilling_fra_nav;


import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvDeltService {

    private final DelingAvCvDAO delingAvCvDAO;
    private final DelingAvCvService delingAvCvService;

    private final StillingFraNavMetrikker stillingFraNavMetrikker;

    @Transactional
    @KafkaListener(topics = "${topic.inn.rekrutteringsbistandStatusoppdatering}", containerFactory = "stringStringKafkaListenerContainerFactory")
    @Timed("kafka_consume_rekrutteringsbistand_statusoppdatering")
    public void consumeRekrutteringsbistandStatusoppdatering(ConsumerRecord<String, String> consumerRecord) {
        String bestillingsId = consumerRecord.key();

        RekrutteringsbistandStatusoppdatering rekrutteringsbistandStatusoppdatering = null;
        try {
            rekrutteringsbistandStatusoppdatering = JsonUtils.fromJson(consumerRecord.value(), RekrutteringsbistandStatusoppdatering.class);
        } catch (Exception ignored) {
        }

        if (rekrutteringsbistandStatusoppdatering == null) {
            log.error("Ugyldig melding bestillingsId: {} på pto.rekrutteringsbistand-statusoppdatering-v1 : {}", bestillingsId, consumerRecord.value());
            stillingFraNavMetrikker.countCvDelt(false, "Ugyldig melding");
            return;
        }

        RekrutteringsbistandStatusoppdatering finalRekrutteringsbistandStatusoppdatering = rekrutteringsbistandStatusoppdatering;
        delingAvCvDAO.hentAktivitetMedBestillingsId(bestillingsId)
                .filter(this::valider)
                .ifPresentOrElse(
                        aktivitetData -> {
                            behandleRekrutteringsbistandoppdatering(
                                    bestillingsId,
                                    finalRekrutteringsbistandStatusoppdatering.type(),
                                    finalRekrutteringsbistandStatusoppdatering.utførtAvNavIdent(),
                                    aktivitetData
                            );
                            stillingFraNavMetrikker.countCvDelt(true, null);
                        },
                        () -> {
                            stillingFraNavMetrikker.countCvDelt(false, "Bestillingsid ikke funnet");
                            log.warn("Fant ikke stillingFraNav aktivitet med bestillingsid {}. Eller aktivitet avbrutt eller historisk", bestillingsId);
                        }
                );
    }

    public void behandleRekrutteringsbistandoppdatering(String bestillingsId, RekrutteringsbistandStatusoppdateringEventType type, String navIdent, AktivitetData aktivitet) {
        Person endretAv = Person.navIdent(Optional.ofNullable(navIdent).orElse("SYSTEM"));

        if (type == RekrutteringsbistandStatusoppdateringEventType.CV_DELT) {
            delingAvCvService.oppdaterSoknadsstatus(aktivitet, Soknadsstatus.CV_DELT, endretAv);
            log.info("Oppdaterte søknadsstatus på aktivitet {}", bestillingsId);
        } else if (type == RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN) {
            throw new NotImplementedException("Det er ikke støtte for IKKE_FATT_JOBBEN ennå");
        }
    }

    private Boolean valider(AktivitetData aktivitetData) {
        AktivitetStatus status = aktivitetData.getStatus();

        if (status == AktivitetStatus.AVBRUTT) {
            log.warn("Stilling fra NAV med bestillingsid: {} er i status AVBRUTT", aktivitetData.getStillingFraNavData().bestillingsId);
            stillingFraNavMetrikker.countCvDelt(false, "Aktivitet AVBRUTT");
            return false;
        }

        if (status == AktivitetStatus.FULLFORT) {
            log.warn("Stilling fra NAV med bestillingsid: {} er i status FULLFORT", aktivitetData.getStillingFraNavData().bestillingsId);
            stillingFraNavMetrikker.countCvDelt(false, "Aktivitet FULLFORT");
            return false;
        }

        if (aktivitetData.getStillingFraNavData().cvKanDelesData == null) {
            log.warn("Stilling fra NAV med bestillingsid: {} har ikke svart", aktivitetData.getStillingFraNavData().bestillingsId);
            this.stillingFraNavMetrikker.countCvDelt(false, "Ikke svart");
            return false;
        }

        if (aktivitetData.getStillingFraNavData().cvKanDelesData.getKanDeles() == Boolean.FALSE) {
            log.error("Stilling fra NAV med bestillingsid: {} har svart NEI", aktivitetData.getStillingFraNavData().bestillingsId);
            stillingFraNavMetrikker.countCvDelt(false, "Svart NEI");
            return false;
        }

        if (aktivitetData.getStillingFraNavData().getSoknadsstatus() == Soknadsstatus.CV_DELT) {
            log.warn("Stilling fra NAV med bestillingsid: {} har allerede status CV_DELT", aktivitetData.getStillingFraNavData().bestillingsId);
            stillingFraNavMetrikker.countCvDelt(false, "Allerede delt");
            return false;
        }

        return true;
    }
}
