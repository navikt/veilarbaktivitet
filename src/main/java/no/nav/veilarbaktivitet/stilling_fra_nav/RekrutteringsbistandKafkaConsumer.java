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
            stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(false, "Ugyldig melding", RekrutteringsbistandStatusoppdateringEventType.UKJENT);
            return;
        }

        RekrutteringsbistandStatusoppdateringEventType type = rekrutteringsbistandStatusoppdatering.type();
        String navIdent = rekrutteringsbistandStatusoppdatering.utførtAvNavIdent();
        String ikkeFattJobbenDetaljer = rekrutteringsbistandStatusoppdatering.detaljer();

        Optional<AktivitetData> optionalAktivitetData = dao.hentAktivitetMedBestillingsId(bestillingsId);

        if (optionalAktivitetData.isPresent()) {
            AktivitetData forrigeAktivitetsdata = optionalAktivitetData.get();
            if (RekrutteringsbistandStatusoppdateringEventType.CV_DELT == type && service.validerCvDelt(forrigeAktivitetsdata)) {
                service.behandleCvDelt(bestillingsId, navIdent, forrigeAktivitetsdata);
            }

            if (RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN == type && service.validerIkkeFattJobben(forrigeAktivitetsdata)) {
                service.behandleIkkeFattJobben(bestillingsId, navIdent, forrigeAktivitetsdata, ikkeFattJobbenDetaljer);
            }
        } else {
            stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(false, "Bestillingsid ikke funnet", type);
            log.warn("Fant ikke stillingFraNav aktivitet med bestillingsid {}, eller aktivitet er historisk", bestillingsId);
        }
    }

}
