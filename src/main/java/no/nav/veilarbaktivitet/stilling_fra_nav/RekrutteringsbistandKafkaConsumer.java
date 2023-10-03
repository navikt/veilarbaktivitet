package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger secureLogs = LoggerFactory.getLogger("SecureLog");

    @KafkaListener(topics = "${topic.inn.rekrutteringsbistandStatusoppdatering}", containerFactory = "stringStringKafkaListenerContainerFactory")
    @Transactional(noRollbackFor = {IngenGjeldendeIdentException.class})
    @Timed("kafka_consume_rekrutteringsbistand_statusoppdatering")
    public void consumeRekrutteringsbistandStatusoppdatering(ConsumerRecord<String, String> consumerRecord) {
        String bestillingsId = consumerRecord.key();

        RekrutteringsbistandStatusoppdatering rekrutteringsbistandStatusoppdatering = null;
        try {
            rekrutteringsbistandStatusoppdatering = JsonUtils.fromJson(consumerRecord.value(), RekrutteringsbistandStatusoppdatering.class);
        } catch (Exception ignored) {
            log.error("Feilet i Json-deserialisering. Se securelogs for payload.");
            secureLogs.error("Feilet i Json-deserialisering. {}.", consumerRecord.value());
        }

        if (rekrutteringsbistandStatusoppdatering == null) {
            log.error("Ugyldig melding bestillingsId: {} på pto.rekrutteringsbistand-statusoppdatering-v1. Se securelogs for payload. ", bestillingsId);
            secureLogs.error("Ugyldig melding bestillingsId: {} på pto.rekrutteringsbistand-statusoppdatering-v1 : {}", bestillingsId, consumerRecord.value());
            stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(false, "Ugyldig melding", RekrutteringsbistandStatusoppdateringEventType.UKJENT);
            return;
        }

        RekrutteringsbistandStatusoppdateringEventType type = rekrutteringsbistandStatusoppdatering.type();

        Person navIdent = rekrutteringsbistandStatusoppdatering.utførtAvNavIdent() == null
                ? Person.systemUser() : Person.navIdent(rekrutteringsbistandStatusoppdatering.utførtAvNavIdent());
        String detaljer = rekrutteringsbistandStatusoppdatering.detaljer();

        Optional<AktivitetData> optionalAktivitetData = dao.hentAktivitetMedBestillingsId(bestillingsId);

        if (optionalAktivitetData.isEmpty()) {
            stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(false, "Bestillingsid ikke funnet", type);
            log.info("Fant ikke stillingFraNav aktivitet med bestillingsid {}, eller aktivitet er historisk", bestillingsId);
            return;
        }

        AktivitetData forrigeAktivitetsdata = optionalAktivitetData.get();
        if (RekrutteringsbistandStatusoppdateringEventType.CV_DELT == type && service.sjekkCvIkkeAlleredeDelt(forrigeAktivitetsdata)) {
            service.behandleCvDelt(bestillingsId, navIdent, forrigeAktivitetsdata);
        } else if (RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN == type && service.sjekkKanSettesTilIkkeFattJobben(forrigeAktivitetsdata)) {
            service.behandleIkkeFattJobben(bestillingsId, navIdent, forrigeAktivitetsdata, detaljer);
        } else if (RekrutteringsbistandStatusoppdateringEventType.FATT_JOBBEN == type && service.sjekkKanSettesTilFattJobben(forrigeAktivitetsdata)) {
            service.behandleFattJobben(bestillingsId, navIdent, forrigeAktivitetsdata, detaljer);
        }
    }

}
