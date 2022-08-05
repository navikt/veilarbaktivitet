package no.nav.veilarbaktivitet.stilling_fra_nav;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvDeltService {

    private final DelingAvCvDAO delingAvCvDAO;
    private final DelingAvCvService delingAvCvService;

    @Transactional
    @KafkaListener(topics = "${topic.inn.rekrutteringsbistandStatusoppdatering}", containerFactory = "stringStringKafkaListenerContainerFactory")
    public void cvdelt(ConsumerRecord<String, String> consumerRecord) {
        String bestillingsId = consumerRecord.key();
        RekrutteringsbistandStatusoppdatering rekrutteringsbistandStatusoppdatering = JsonUtils.fromJson(consumerRecord.value(), RekrutteringsbistandStatusoppdatering.class);
        Person endretAv = Person.navIdent(rekrutteringsbistandStatusoppdatering.navIdent());

        switch (rekrutteringsbistandStatusoppdatering.type()) {
            case CV_DELT -> delingAvCvDAO.hentAktivitetMedBestillingsId(bestillingsId).ifPresentOrElse(
                    aktivitet -> {
                        delingAvCvService.oppdaterSoknadsstatus(aktivitet, Soknadsstatus.CV_DELT, endretAv);
                        log.info("Oppdaterte søknadsstatus på aktivitet {}", bestillingsId);
                    },
                    () -> log.warn("Fant ikke aktivitet {}", bestillingsId));

            case IKKE_FATT_JOBBEN -> throw new NotImplementedException("Det er ikke støtte for IKKE_FATT_JOBBEN ennå");

            default -> throw new IllegalArgumentException(
                    String.format("Uventet type: %s. Mulige typer: %s",
                            rekrutteringsbistandStatusoppdatering.type(),
                            Arrays.toString(RekrutteringsbistandStatusoppdateringEventType.values())
                    ));
        }
    }
}
