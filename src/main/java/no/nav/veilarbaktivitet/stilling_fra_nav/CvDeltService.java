package no.nav.veilarbaktivitet.stilling_fra_nav;


import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
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
        } catch (Exception ignored) { }

        if (rekrutteringsbistandStatusoppdatering == null) {
            log.error("Ugyldig melding bestillingsId: {} på pto.rekrutteringsbistand-statusoppdatering-v1 : {}", bestillingsId, consumerRecord.value());
            stillingFraNavMetrikker.countCvDelt(false, "Ugyldig melding");
            return;
        }

        RekrutteringsbistandStatusoppdatering finalRekrutteringsbistandStatusoppdatering = rekrutteringsbistandStatusoppdatering;
        delingAvCvDAO.hentAktivitetMedBestillingsId(bestillingsId).ifPresentOrElse(
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
                    log.warn("Fant ikke stillingFraNav aktivitet med bestillingsid {}. Eller aktivitet avbrutt eller historisk", bestillingsId)  ;
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
}
