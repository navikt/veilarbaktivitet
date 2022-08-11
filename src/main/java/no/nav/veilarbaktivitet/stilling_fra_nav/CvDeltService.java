package no.nav.veilarbaktivitet.stilling_fra_nav;


import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvDeltService {

    public static final String CV_DELT_DITT_NAV_TEKST = "NAV har delt din CV med arbeidsgiver på denne stillingen";
    public static final String CV_DELT_EPOST_TITTEL = "Hei. Du har fått en ny beskjed på Ditt NAV.";
    public static final String CV_DELT_EPOST_BODY = "Logg inn og se hva beskjeden gjelder. \nVennlig hilsen NAV";
    public static final String CV_DELT_SMS_TEKST = "Hei. Du har fått en ny beskjed på Ditt NAV. Logg inn og se hva beskjeden gjelder. Vennlig hilsen NAV";
    private final DelingAvCvDAO delingAvCvDAO;
    private final DelingAvCvService delingAvCvService;

    private final StillingFraNavMetrikker stillingFraNavMetrikker;

    private final BrukernotifikasjonService brukernotifikasjonService;

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
                            maybeBestillBrukernotifikasjon(aktivitetData);
                            stillingFraNavMetrikker.countCvDelt(true, null);
                        },
                        () -> {
                            stillingFraNavMetrikker.countCvDelt(false, "Bestillingsid ikke funnet");
                            log.warn("Fant ikke stillingFraNav aktivitet med bestillingsid {}. Eller aktivitet avbrutt eller historisk", bestillingsId);
                        }
                );
    }

    private void maybeBestillBrukernotifikasjon(AktivitetData aktivitetData) {
        if (brukernotifikasjonService.finnesBrukernotifikasjonMedVarselTypeForAktivitet(aktivitetData.getId(), VarselType.CV_DELT)) {
            log.warn("Brukernotifikasjon er allerede sendt for CV_DELT på bestillingsid {}", aktivitetData.getStillingFraNavData().bestillingsId);
            return;
        }

        brukernotifikasjonService.opprettVarselPaaAktivitet(
                aktivitetData.getId(),
                aktivitetData.getVersjon(),
                Person.aktorId(aktivitetData.getAktorId()),
                CV_DELT_DITT_NAV_TEKST,
                VarselType.CV_DELT,
                CV_DELT_EPOST_TITTEL,
                CV_DELT_EPOST_BODY,
                CV_DELT_SMS_TEKST
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
