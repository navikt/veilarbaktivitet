package no.nav.veilarbaktivitet.stilling_fra_nav;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RekrutteringsbistandStatusoppdateringService {
    public static final String CV_DELT_DITT_NAV_TEKST = "NAV har delt din CV med arbeidsgiver på denne stillingen";
    public static final String IKKE_FATT_JOBBEN_TEKST = "NAV har gjort en oppdatering på denne stillingen";
    private final AktivitetDAO aktivitetDAO;
    private final StillingFraNavMetrikker stillingFraNavMetrikker;
    private final BrukernotifikasjonService brukernotifikasjonService;

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
                    case IKKE_FATT_JOBBEN -> IKKE_FATT_JOBBEN_TEKST;
                    default -> "";
                },
                varselType);
    }

    public void behandleCvDelt(String bestillingsId, String navIdent, AktivitetData aktivitet) {
        Person endretAv = Person.navIdent(Optional.ofNullable(navIdent).orElse("SYSTEM"));
        var nyStillingFraNavData = aktivitet.getStillingFraNavData().withSoknadsstatus(Soknadsstatus.CV_DELT);
        var nyAktivitet = aktivitet.toBuilder()
                .lagtInnAv(endretAv.tilBrukerType())
                .stillingFraNavData(nyStillingFraNavData)
                .transaksjonsType(AktivitetTransaksjonsType.SOKNADSSTATUS_ENDRET)
                .endretAv(endretAv.get())
                .build();
        aktivitetDAO.oppdaterAktivitet(nyAktivitet);
        log.info("Oppdaterte søknadsstatus på aktivitet {}", bestillingsId);
        stillingFraNavMetrikker.countCvDelt(true, null);
        maybeBestillBrukernotifikasjon(aktivitet, VarselType.CV_DELT);
    }

    public void behandleIkkeFattJobben(String bestillingsId, String navIdent, AktivitetData aktivitet) {
        Person endretAv = Person.navIdent(Optional.ofNullable(navIdent).orElse("SYSTEM"));
        var nyStillingFraNavData = aktivitet.getStillingFraNavData().withSoknadsstatus(Soknadsstatus.AVSLAG);
        var nyAktivitet = aktivitet.toBuilder()
                .lagtInnAv(endretAv.tilBrukerType())
                .stillingFraNavData(nyStillingFraNavData)
                .transaksjonsType(AktivitetTransaksjonsType.IKKE_FATT_JOBBEN)
                .endretAv(endretAv.get())
                .status(AktivitetStatus.FULLFORT)
                .build();
        aktivitetDAO.oppdaterAktivitet(nyAktivitet);
        log.info("Oppdaterte søknadsstatus og aktivitetstatus på aktivitet {}", bestillingsId);
        stillingFraNavMetrikker.countIkkeFattJobben(true, null);
        // TODO Trello-oppgave "Ny brukernotifikasjon_Ikke fått jobben"
//            maybeBestillBrukernotifikasjon(aktivitet, VarselType.IKKE_FATT_JOBBEN);
    }

    private Boolean validerStillingFraNavOppdatering(AktivitetData aktivitetData) {
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

        return true;
    }

    Boolean validerCvDelt(AktivitetData aktivitetData) {
        if (aktivitetData.getStillingFraNavData().getSoknadsstatus() == Soknadsstatus.CV_DELT) {
            log.warn("Stilling fra NAV med bestillingsid: {} har allerede status CV_DELT", aktivitetData.getStillingFraNavData().bestillingsId);
            stillingFraNavMetrikker.countCvDelt(false, "Allerede delt");
            return false;
        }

        return validerStillingFraNavOppdatering(aktivitetData);
    }

    Boolean validerIkkeFattJobben(AktivitetData aktivitetData) {
        return validerStillingFraNavOppdatering(aktivitetData);
    }

}
