package no.nav.veilarbaktivitet.stilling_fra_nav;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.brukernotifikasjon.MinsideVarselService;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselType;
import no.nav.veilarbaktivitet.brukernotifikasjon.opprettVarsel.AktivitetVarsel;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;

import static java.util.Arrays.asList;

@Service
@RequiredArgsConstructor
@Slf4j
public class RekrutteringsbistandStatusoppdateringService {
    public static final String CV_DELT_DITT_NAV_TEKST = "Nav har delt din CV med arbeidsgiver på denne stillingen";
    public static final String IKKE_FATT_JOBBEN_TEKST = "Nav har oppdatert denne stillingsannonsen";
    private final AktivitetDAO aktivitetDAO;
    private final StillingFraNavMetrikker stillingFraNavMetrikker;
    private final MinsideVarselService brukernotifikasjonService;

    private void maybeBestillBrukernotifikasjon(AktivitetData aktivitetData, VarselType varselType) {
        if (brukernotifikasjonService.finnesBrukernotifikasjonMedVarselTypeForAktivitet(aktivitetData.getId(), varselType)) {
            log.info("Brukernotifikasjon er allerede sendt for {} på bestillingsid {}", varselType, aktivitetData.getStillingFraNavData().bestillingsId);
            return;
        }
        try {
            brukernotifikasjonService.opprettVarselPaaAktivitet(new AktivitetVarsel(
                    aktivitetData.getId(),
                    aktivitetData.getVersjon(),
                    aktivitetData.getAktorId(),
                    switch (varselType) {
                        case CV_DELT -> CV_DELT_DITT_NAV_TEKST;
                        case IKKE_FATT_JOBBEN -> IKKE_FATT_JOBBEN_TEKST;
                        default -> "";
                    },
                    varselType,
                    null,null,null
                )
            );
        } catch (IngenGjeldendeIdentException e) {
            log.warn("Fikk ikke opprettet brukernotifikasjon pga ugyldig aktørId={}. Fortsetter behandling", aktivitetData.getAktorId());
        }
    }

    public void behandleCvDelt(String bestillingsId, Person endretAv, AktivitetData aktivitet) {
        var nyStillingFraNavData = aktivitet.getStillingFraNavData().withSoknadsstatus(Soknadsstatus.CV_DELT);
        var nyAktivitet = aktivitet.toBuilder()
                .endretAvType(endretAv.tilInnsenderType())
                .stillingFraNavData(nyStillingFraNavData)
                .transaksjonsType(AktivitetTransaksjonsType.SOKNADSSTATUS_ENDRET)
                .endretDato(aktivitet.getEndretDato())
                .endretAv(endretAv.get())
                .build();
        aktivitetDAO.oppdaterAktivitet(nyAktivitet);
        log.info("Oppdaterte søknadsstatus på aktivitet {}", bestillingsId);
        stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(true, null, RekrutteringsbistandStatusoppdateringEventType.CV_DELT);
        maybeBestillBrukernotifikasjon(aktivitet, VarselType.CV_DELT);
    }

    private AktivitetData ferdigstillAktivitetMedStatus(Soknadsstatus soknadsstatus, Person endretAv, AktivitetData aktivitet, String detaljer) {
        var nyStillingFraNavData = aktivitet.getStillingFraNavData()
                .withDetaljer(detaljer)
                .withSoknadsstatus(soknadsstatus);
        AktivitetStatus nyStatus = asList(AktivitetStatus.FULLFORT, AktivitetStatus.AVBRUTT).contains(aktivitet.getStatus()) ?
                aktivitet.getStatus() : AktivitetStatus.FULLFORT;
        return aktivitet.toBuilder()
                .endretAvType(endretAv.tilInnsenderType())
                .stillingFraNavData(nyStillingFraNavData)
                .transaksjonsType(soknadsstatus.equals(Soknadsstatus.FATT_JOBBEN)
                        ? AktivitetTransaksjonsType.FATT_JOBBEN
                        : AktivitetTransaksjonsType.IKKE_FATT_JOBBEN)
                .endretAv(endretAv.get())
                .endretDato(aktivitet.getEndretDato())
                .status(nyStatus)
                .build();
    }

    public void behandleIkkeFattJobben(String bestillingsId, Person endretAv, AktivitetData aktivitet, String detaljer) {
        var nyAktivitet = ferdigstillAktivitetMedStatus(Soknadsstatus.IKKE_FATT_JOBBEN, endretAv, aktivitet, detaljer);
        aktivitetDAO.oppdaterAktivitet(nyAktivitet);
        log.info("Oppdaterte søknadsstatus og aktivitetstatus på aktivitet {}", bestillingsId);
        stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(true, "", RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN);
        maybeBestillBrukernotifikasjon(aktivitet, VarselType.IKKE_FATT_JOBBEN);
    }

    public void behandleFattJobben(String bestillingsId, Person endretAv, AktivitetData aktivitet, String detaljer) {
        var nyAktivitet = ferdigstillAktivitetMedStatus(Soknadsstatus.FATT_JOBBEN, endretAv, aktivitet, detaljer);
        aktivitetDAO.oppdaterAktivitet(nyAktivitet);
        log.info("Oppdaterte søknadsstatus og aktivitetstatus på aktivitet {}", bestillingsId);
        stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(true, "", RekrutteringsbistandStatusoppdateringEventType.FATT_JOBBEN);
    }

    private boolean sjekkCVBurdeEgentligIkkeVærtDelt(AktivitetData aktivitetData, RekrutteringsbistandStatusoppdateringEventType type) {
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

    boolean sjekkCvIkkeAlleredeDelt(AktivitetData aktivitetData) {
        if (!sjekkCVBurdeEgentligIkkeVærtDelt(aktivitetData, RekrutteringsbistandStatusoppdateringEventType.CV_DELT)) return false;

        if (aktivitetData.getStillingFraNavData().getSoknadsstatus() == Soknadsstatus.CV_DELT) {
            log.info("Stilling fra NAV med bestillingsid: {} har allerede status CV_DELT", aktivitetData.getStillingFraNavData().bestillingsId);
            stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(false, "Allerede delt", RekrutteringsbistandStatusoppdateringEventType.CV_DELT);
            return false;
        }

        return true;
    }

    boolean sjekkKanSettesTilIkkeFattJobben(AktivitetData aktivitetData) {
        if (!sjekkCVBurdeEgentligIkkeVærtDelt(aktivitetData, RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN)) return false;

        if (aktivitetData.getStillingFraNavData().getSoknadsstatus() == Soknadsstatus.IKKE_FATT_JOBBEN) {
            log.info("Stilling fra NAV med bestillingsid: {} har allerede status IKKE_FATT_JOBBEN", aktivitetData.getStillingFraNavData().bestillingsId);
            stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(false, "Allerede ikke fått jobben", RekrutteringsbistandStatusoppdateringEventType.IKKE_FATT_JOBBEN);
            return false;
        }

        return true;
    }

    boolean sjekkKanSettesTilFattJobben(AktivitetData aktivitetData) {
        if (!sjekkCVBurdeEgentligIkkeVærtDelt(aktivitetData, RekrutteringsbistandStatusoppdateringEventType.FATT_JOBBEN)) return false;

        if (aktivitetData.getStillingFraNavData().getSoknadsstatus() == Soknadsstatus.FATT_JOBBEN) {
            log.info("Stilling fra NAV med bestillingsid: {} har allerede status FATT_JOBBEN", aktivitetData.getStillingFraNavData().bestillingsId);
            stillingFraNavMetrikker.countRekrutteringsbistandStatusoppdatering(false, "Allerede fått jobben", RekrutteringsbistandStatusoppdateringEventType.FATT_JOBBEN);
            return false;
        }

        return true;
    }

}
