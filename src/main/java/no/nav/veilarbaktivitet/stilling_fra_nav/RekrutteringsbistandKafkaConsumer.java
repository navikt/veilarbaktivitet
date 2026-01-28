package no.nav.veilarbaktivitet.stilling_fra_nav;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static no.nav.veilarbaktivitet.config.TeamLog.teamLog;

@Service
@RequiredArgsConstructor
@Slf4j
public class RekrutteringsbistandKafkaConsumer {
    private final RekrutteringsbistandStatusoppdateringService service;
    private final RekrutteringsbistandStatusoppdateringDAO dao;
    private final StillingFraNavMetrikker stillingFraNavMetrikker;

    @KafkaListener(topics = "${topic.inn.rekrutteringsbistandStatusoppdatering}", containerFactory = "stringStringKafkaListenerContainerFactory", autoStartup = "${app.kafka.enabled:false}")
    @Transactional(noRollbackFor = {IngenGjeldendeIdentException.class})
    @Timed("kafka_consume_rekrutteringsbistand_statusoppdatering")
    public void consumeRekrutteringsbistandStatusoppdatering(ConsumerRecord<String, String> consumerRecord) {
        String bestillingsId = consumerRecord.key();

        RekrutteringsbistandStatusoppdatering rekrutteringsbistandStatusoppdatering = null;
        try {
            rekrutteringsbistandStatusoppdatering = JsonUtils.fromJson(consumerRecord.value(), RekrutteringsbistandStatusoppdatering.class);
        } catch (Exception ignored) {
            log.error("Feilet i Json-deserialisering. Se teamLogs for payload.");
            teamLog.error("Feilet i Json-deserialisering. {}.", consumerRecord.value());
        }

        if (rekrutteringsbistandStatusoppdatering == null) {
            log.error("Ugyldig melding bestillingsId: {} på pto.rekrutteringsbistand-statusoppdatering-v1. Se teamLogs for payload. ", bestillingsId);
            teamLog.error("Ugyldig melding bestillingsId: {} på pto.rekrutteringsbistand-statusoppdatering-v1 : {}", bestillingsId, consumerRecord.value());
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

        AktivitetData forrigeAktivitetsdata = optionalAktivitetData.get()
                .withEndretDato(DateUtils.zonedDateTimeToDate(rekrutteringsbistandStatusoppdatering.tidspunkt()));
        if (RekrutteringsbistandStatusoppdateringEventType.CV_DELT == type && service.sjekkCvIkkeAlleredeDelt(forrigeAktivitetsdata)) {
            service.behandleCvDelt(bestillingsId, navIdent, forrigeAktivitetsdata);
        } else if (RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN == type && service.sjekkKanSettesTilIkkeFattJobben(forrigeAktivitetsdata)) {
            service.behandleIkkeFattJobben(bestillingsId, navIdent, forrigeAktivitetsdata, detaljer);
        } else if (RekrutteringsbistandStatusoppdateringEventType.FATT_JOBBEN == type && service.sjekkKanSettesTilFattJobben(forrigeAktivitetsdata)) {
            service.behandleFattJobben(bestillingsId, navIdent, forrigeAktivitetsdata, detaljer);
        }
    }

}
