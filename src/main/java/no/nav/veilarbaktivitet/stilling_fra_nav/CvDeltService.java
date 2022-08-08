package no.nav.veilarbaktivitet.stilling_fra_nav;


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

    @Transactional
    @KafkaListener(topics = "${topic.inn.rekrutteringsbistandStatusoppdatering}", containerFactory = "stringStringKafkaListenerContainerFactory")
    public void consumeRekrutteringsbistandStatusoppdatering(ConsumerRecord<String, String> consumerRecord) {
        String bestillingsId = consumerRecord.key();
        RekrutteringsbistandStatusoppdatering rekrutteringsbistandStatusoppdatering = JsonUtils.fromJson(consumerRecord.value(), RekrutteringsbistandStatusoppdatering.class);

        delingAvCvDAO.hentAktivitetMedBestillingsId(bestillingsId).ifPresentOrElse(
                aktivitetData -> behandleRekrutteringsbistandoppdatering(
                        bestillingsId,
                        rekrutteringsbistandStatusoppdatering.type(),
                        rekrutteringsbistandStatusoppdatering.utførtAvNavIdent(),
                        aktivitetData
                ),
                () -> log.warn("Fant ikke aktivitet {}", bestillingsId)
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
