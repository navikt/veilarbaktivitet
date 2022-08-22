package no.nav.veilarbaktivitet.stilling_fra_nav;


import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvDeltService {

    public static final String CV_DELT_DITT_NAV_TEKST = "NAV har delt din CV med arbeidsgiver på denne stillingen";
    public static final String EN_TEKST_VI_KAN_FA_FRA_PO = "Det ble ikke deg denne gangen :-/";
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
            log.debug("Feilet i fromJson");
        }

        if (rekrutteringsbistandStatusoppdatering == null) {
            log.error("Ugyldig melding bestillingsId: {} på pto.rekrutteringsbistand-statusoppdatering-v1 : {}", bestillingsId, consumerRecord.value());
            stillingFraNavMetrikker.countCvDelt(false, "Ugyldig melding");
            return;
        }

        RekrutteringsbistandStatusoppdatering finalRekrutteringsbistandStatusoppdatering = rekrutteringsbistandStatusoppdatering;

        Optional<AktivitetData> optionalAktivitetData = delingAvCvDAO.hentAktivitetMedBestillingsId(bestillingsId);

        if (optionalAktivitetData.isEmpty()) {
            stillingFraNavMetrikker.countCvDelt(false, "Bestillingsid ikke funnet");
            log.warn("Fant ikke stillingFraNav aktivitet med bestillingsid {}, eller aktivitet er historisk", bestillingsId);
        } else {
            optionalAktivitetData
                    .filter(this::valider)
                    .ifPresent(
                            aktivitetData ->
                                    behandleRekrutteringsbistandoppdatering(
                                            bestillingsId,
                                            finalRekrutteringsbistandStatusoppdatering.type(),
                                            finalRekrutteringsbistandStatusoppdatering.utførtAvNavIdent(),
                                            aktivitetData
                                    )
                    );
        }
    }

    private void maybeBestillBrukernotifikasjon(AktivitetData aktivitetData, VarselType varselType) {
        if (brukernotifikasjonService.finnesBrukernotifikasjonMedVarselTypeForAktivitet(aktivitetData.getId(), varselType)) {
            log.warn("Brukernotifikasjon er allerede sendt for {} på bestillingsid {}", varselType, aktivitetData.getStillingFraNavData().bestillingsId);
            return;
        }
        brukernotifikasjonService.opprettVarselPaaAktivitet(
                aktivitetData.getId(),
                aktivitetData.getVersjon(),
                Person.aktorId(aktivitetData.getAktorId()),
                switch (varselType) {
                    case CV_DELT -> CV_DELT_DITT_NAV_TEKST;
                    case IKKE_FATT_JOBBEN -> EN_TEKST_VI_KAN_FA_FRA_PO;
                    default -> "";
                },
                varselType);
    }

    public void behandleRekrutteringsbistandoppdatering(String bestillingsId, RekrutteringsbistandStatusoppdateringEventType type, String navIdent, AktivitetData aktivitet) {
        Person endretAv = Person.navIdent(Optional.ofNullable(navIdent).orElse("SYSTEM"));

        if (type == RekrutteringsbistandStatusoppdateringEventType.CV_DELT) {
            delingAvCvService.oppdaterSoknadsstatus(aktivitet, Soknadsstatus.CV_DELT, endretAv);
            log.info("Oppdaterte søknadsstatus på aktivitet {}", bestillingsId);
            stillingFraNavMetrikker.countCvDelt(true, null);
            maybeBestillBrukernotifikasjon(aktivitet, VarselType.CV_DELT);
        } else if (type == RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN) {
            delingAvCvService.ikkeFattJobben(aktivitet, endretAv);
            log.info("Oppdaterte søknadsstatus og aktivitetstatus på aktivitet {}", bestillingsId);
            stillingFraNavMetrikker.countIkkeFattJobben(true, null);
            maybeBestillBrukernotifikasjon(aktivitet, VarselType.IKKE_FATT_JOBBEN);
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
            log.info("Stilling fra NAV med bestillingsid: {} er i status FULLFORT. Setter CV_DELT etikett", aktivitetData.getStillingFraNavData().bestillingsId);
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
