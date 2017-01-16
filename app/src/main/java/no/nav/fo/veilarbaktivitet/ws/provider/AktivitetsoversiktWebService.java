package no.nav.fo.veilarbaktivitet.ws.provider;

import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.fo.veilarbaktivitet.db.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.ws.consumer.AktoerConsumer;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

import static java.util.Optional.of;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.PLANLAGT;
import static no.nav.fo.veilarbaktivitet.domain.Innsender.BRUKER;

// TODO
//@WebService(
//        name = "???",
//        targetNamespace = "???"
//)
//@Service
@Component
public class AktivitetsoversiktWebService {


    @Inject
    private AktoerConsumer aktoerConsumer;

    @Inject
    private AktivitetDAO aktivitetDAO;

    //@Override
    public WSHentAktiviteterResponse hentAktiviteter(WSHentAktiviteterRequest request) {
        String aktorId = hentAktoerIdForIdent(request.ident);

        WSHentAktiviteterResponse wsHentAktiviteterResponse = new WSHentAktiviteterResponse();
        aktivitetDAO.hentStillingsAktiviteterForAktorId(aktorId).stream().map(this::somWSAktivitet).forEach(wsHentAktiviteterResponse.aktivitetsoversikt.stillingsAktiviteter::add);
        aktivitetDAO.hentEgenAktiviteterForAktorId(aktorId).stream().map(this::somWSAktivitet).forEach(wsHentAktiviteterResponse.aktivitetsoversikt.egenAktiviteter::add);
        return wsHentAktiviteterResponse;
    }

    //@Override
    public WSOpprettNyStillingAktivitetResponse opprettNyStillingAktivitet(WSOpprettNyStillingAktivitetRequest request) {
        WSOpprettNyStillingAktivitetResponse wsOpprettNyStillingAktivitetResponse = new WSOpprettNyStillingAktivitetResponse();
        wsOpprettNyStillingAktivitetResponse.stillingsaktivitet = of(request)
                .map(this::somStillingAktivitet)
                .map(aktivitetDAO::opprettStillingAktivitet)
                .map(this::somWSAktivitet)
                .get();

        return wsOpprettNyStillingAktivitetResponse;
    }

    //@Override
    public WSOpprettNyEgenAktivitetResponse opprettNyEgenAktivitet(WSOpprettNyEgenAktivitetRequest request) {
        WSOpprettNyEgenAktivitetResponse opprettNyEgenAktivitetResponse = new WSOpprettNyEgenAktivitetResponse();
        opprettNyEgenAktivitetResponse.egenaktiviteter = of(request)
                .map(this::somEgenAktivitet)
                .map(aktivitetDAO::opprettEgenAktivitet)
                .map(this::somWSAktivitet)
                .get();

        return opprettNyEgenAktivitetResponse;
    }

    private String hentAktoerIdForIdent(String ident) {
        return aktoerConsumer.hentAktoerIdForIdent(ident)
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

    private StillingsSoekAktivitet somStillingAktivitet(WSOpprettNyStillingAktivitetRequest request) {
        return new StillingsSoekAktivitet()
                .setStillingsoek(new Stillingsoek()
                    .setStillingsoekEtikett(StillingsoekEtikett.values()[0]) // TODO
                )
                .setAktivitet(new Aktivitet()
                        .setAktorId(hentAktoerIdForIdent(request.ident))

                        // TODO
                        .setStatus(PLANLAGT)
                        .setLagtInnAv(BRUKER)
                        // TODO
                );
    }

    private EgenAktivitet somEgenAktivitet(WSOpprettNyEgenAktivitetRequest request) {
        return new EgenAktivitet()
                .setAktivitet(new Aktivitet()
                        .setAktorId(hentAktoerIdForIdent(request.ident))

                        // TODO
                        .setStatus(PLANLAGT)
                        .setLagtInnAv(BRUKER)
                        // TODO
                );
    }

    private WSStillingsAktivitet somWSAktivitet(StillingsSoekAktivitet stillingsSoekAktivitet) {
        return new WSStillingsAktivitet();
    }

    private WSEgenAktivitet somWSAktivitet(EgenAktivitet egenAktivitet) {
        return new WSEgenAktivitet();
    }

    //@Override
    public void ping() {
    }

    // TODO disse blir definert i tjenestespesifikasjonen når den er ferdig!

    public static class WSHentAktiviteterRequest{
        public String ident;
    }

    @XmlRootElement
    public static class WSHentAktiviteterResponse{

        public WSAktivitetsoversikt aktivitetsoversikt = new WSAktivitetsoversikt();
    }

    public static class WSAktivitetsoversikt {

        public List<WSStillingsAktivitet> stillingsAktiviteter = new ArrayList<>();
        public List<WSEgenAktivitet> egenAktiviteter = new ArrayList<>();
    }

    public static class WSStillingsAktivitet {

    }

    public static class WSEgenAktivitet {

    }

    public static class WSOpprettNyStillingAktivitetRequest{
        public String ident;
        public WSStillingsAktivitet stillingaktivitet;
    }

    public static class WSOpprettNyStillingAktivitetResponse{
        public WSStillingsAktivitet stillingsaktivitet;
    }

    public static class WSOpprettNyEgenAktivitetRequest {
        public String ident;
        public WSEgenAktivitet egenaktivitet;
    }

    @XmlRootElement
    public static class WSOpprettNyEgenAktivitetResponse {
        public WSEgenAktivitet egenaktiviteter;
    }


}

