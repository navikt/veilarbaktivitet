package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RekrutteringsbistandKafkaConsumer {
    private final RekrutteringsbistandStatusoppdateringService service;
    private final RekrutteringsbistandStatusoppdateringDAO dao;
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
            log.debug("Feilet i Json-deserialisering");
        }

        if (rekrutteringsbistandStatusoppdatering == null) {
            log.error("Ugyldig melding bestillingsId: {} på pto.rekrutteringsbistand-statusoppdatering-v1 : {}", bestillingsId, consumerRecord.value());
            stillingFraNavMetrikker.countCvDelt(false, "Ugyldig melding");
            return;
        }

        RekrutteringsbistandStatusoppdateringEventType type = rekrutteringsbistandStatusoppdatering.type();
        String navIdent = rekrutteringsbistandStatusoppdatering.utførtAvNavIdent();

        Optional<AktivitetData> optionalAktivitetData = dao.hentAktivitetMedBestillingsId(bestillingsId);

        if (optionalAktivitetData.isEmpty()) {
            stillingFraNavMetrikker.countCvDelt(false, "Bestillingsid ikke funnet");
            log.warn("Fant ikke stillingFraNav aktivitet med bestillingsid {}, eller aktivitet er historisk", bestillingsId);
        } else {
            AktivitetData aktivitetData = optionalAktivitetData.get();
            if (RekrutteringsbistandStatusoppdateringEventType.CV_DELT == type && service.validerCvDelt(aktivitetData)) {
                service.behandleCvDelt(bestillingsId, navIdent, aktivitetData);
            }

            if (RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN == type && service.validerIkkeFattJobben(aktivitetData)) {
                service.behandleIkkeFattJobben(bestillingsId, navIdent, aktivitetData);
            }
        }
    }

}
