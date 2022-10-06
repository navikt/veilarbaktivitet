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
import no.nav.veilarbaktivitet.person.UgyldigPersonIdentException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.util.Arrays.asList;

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
        try {
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
        } catch (UgyldigPersonIdentException e) {
            log.warn("Fikk ikke opprettet brukernotifikasjon pga ugyldig aktørId={}. Fortsetter behandling", aktivitetData.getAktorId());
        }
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
        stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(true, null, RekrutteringsbistandStatusoppdateringEventType.CV_DELT);
        maybeBestillBrukernotifikasjon(aktivitet, VarselType.CV_DELT);
    }

    public void behandleIkkeFattJobben(String bestillingsId, String navIdent, AktivitetData aktivitet, String ikkefattjobbendetaljer) {
        Person endretAv = Person.navIdent(Optional.ofNullable(navIdent).orElse("SYSTEM"));
        var nyStillingFraNavData = aktivitet.getStillingFraNavData()
                .withIkkefattjobbendetaljer(ikkefattjobbendetaljer)
                .withSoknadsstatus(Soknadsstatus.IKKE_FATT_JOBBEN);
        AktivitetStatus nyStatus = asList(AktivitetStatus.FULLFORT, AktivitetStatus.AVBRUTT).contains(aktivitet.getStatus()) ?
                aktivitet.getStatus() : AktivitetStatus.FULLFORT;
        var nyAktivitet = aktivitet.toBuilder()
                .lagtInnAv(endretAv.tilBrukerType())
                .stillingFraNavData(nyStillingFraNavData)
                .transaksjonsType(AktivitetTransaksjonsType.IKKE_FATT_JOBBEN)
                .endretAv(endretAv.get())
                .status(nyStatus)
                .build();
        aktivitetDAO.oppdaterAktivitet(nyAktivitet);
        log.info("Oppdaterte søknadsstatus og aktivitetstatus på aktivitet {}", bestillingsId);
        stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(true, "", RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN);
        maybeBestillBrukernotifikasjon(aktivitet, VarselType.IKKE_FATT_JOBBEN);
    }

    private boolean validerStillingFraNavOppdatering(AktivitetData aktivitetData, RekrutteringsbistandStatusoppdateringEventType type) {
        if (aktivitetData.getStillingFraNavData().cvKanDelesData == null) {
            log.warn("Stilling fra NAV med bestillingsid: {} har ikke svart", aktivitetData.getStillingFraNavData().bestillingsId);
            this.stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(false, "Ikke svart", type);
            return false;
        }

        if (aktivitetData.getStillingFraNavData().cvKanDelesData.getKanDeles() == Boolean.FALSE) {
            log.error("Stilling fra NAV med bestillingsid: {} har svart NEI", aktivitetData.getStillingFraNavData().bestillingsId);
            stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(false, "Svart NEI", type);
            return false;
        }

        if (aktivitetData.getStatus() == AktivitetStatus.AVBRUTT) {
            log.warn("Stilling fra NAV med bestillingsid: {} er i status AVBRUTT", aktivitetData.getStillingFraNavData().bestillingsId);
            stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(false, "Aktivitet AVBRUTT", type);
            return false;
        }

        return true;
    }

    boolean validerCvDelt(AktivitetData aktivitetData) {
        if (!validerStillingFraNavOppdatering(aktivitetData, RekrutteringsbistandStatusoppdateringEventType.CV_DELT)) return false;

        if (aktivitetData.getStillingFraNavData().getSoknadsstatus() == Soknadsstatus.CV_DELT) {
            log.warn("Stilling fra NAV med bestillingsid: {} har allerede status CV_DELT", aktivitetData.getStillingFraNavData().bestillingsId);
            stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(false, "Allerede delt", RekrutteringsbistandStatusoppdateringEventType.CV_DELT);
            return false;
        }

        return true;
    }

    boolean validerIkkeFattJobben(AktivitetData aktivitetData) {
        if (!validerStillingFraNavOppdatering(aktivitetData, RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN)) return false;

        if (aktivitetData.getStillingFraNavData().getSoknadsstatus() == Soknadsstatus.IKKE_FATT_JOBBEN) {
            log.warn("Stilling fra NAV med bestillingsid: {} har allerede status IKKE_FATT_JOBBEN", aktivitetData.getStillingFraNavData().bestillingsId);
            stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(false, "Allerede ikke fått jobben", RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN);
            return false;
        }

        return true;
    }

}
