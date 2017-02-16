package no.nav.fo.veilarbaktivitet.ws.provider;

import lombok.val;
import no.nav.fo.veilarbaktivitet.db.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.db.EndringsLoggDAO;
import no.nav.fo.veilarbaktivitet.ws.consumer.AktoerConsumer;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.BehandleAktivitetsplanV1;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.HentAktivitetsplanSikkerhetsbegrensing;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.OpprettNyStillingAktivitetSikkerhetsbegrensing;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.OpprettNyStillingAktivitetUgyldigInput;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitetsplan;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.*;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.jws.WebService;

import static java.util.Optional.of;

@WebService
@Service
public class AktivitetsoversiktWebService implements BehandleAktivitetsplanV1 {

    @Inject
    private AktoerConsumer aktoerConsumer;

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Inject
    private EndringsLoggDAO endringsLoggDAO;

    @Inject
    private AktivitetsoversiktWebServiceTransformer aktivitetsoversiktWebServiceTransformer;

    @Override
    public OpprettNyEgenAktivitetResponse opprettNyEgenAktivitet(OpprettNyEgenAktivitetRequest opprettNyEgenAktivitetRequest) {
        return of(opprettNyEgenAktivitetRequest)
                .map(aktivitetsoversiktWebServiceTransformer::mapTilEgenAktivitetData)
                .map(aktivitetDAO::opprettEgenAktivitet)
                .map(aktivitetsoversiktWebServiceTransformer::mapTilAktivitet)
                .map(aktivitetsoversiktWebServiceTransformer::somOpprettNyEgenAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public OpprettNyStillingAktivitetResponse opprettNyStillingAktivitet(OpprettNyStillingAktivitetRequest opprettNyStillingAktivitetRequest) throws OpprettNyStillingAktivitetSikkerhetsbegrensing, OpprettNyStillingAktivitetUgyldigInput {
        return of(opprettNyStillingAktivitetRequest)
                .map(aktivitetsoversiktWebServiceTransformer::mapTilStillingAktivitetData)
                .map(aktivitetDAO::opprettStillingAktivitet)
                .map(aktivitetsoversiktWebServiceTransformer::mapTilAktivitet)
                .map(aktivitetsoversiktWebServiceTransformer::somOpprettNyStillingAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public HentAktivitetsplanResponse hentAktivitetsplan(HentAktivitetsplanRequest hentAktivitetsplanRequest) throws HentAktivitetsplanSikkerhetsbegrensing {
        String aktorId = hentAktoerIdForIdent(hentAktivitetsplanRequest.getPersonident());

        HentAktivitetsplanResponse wsHentAktiviteterResponse = new HentAktivitetsplanResponse();
        Aktivitetsplan aktivitetsplan = new Aktivitetsplan();
        wsHentAktiviteterResponse.setAktivitetsplan(aktivitetsplan);

        aktivitetDAO.hentStillingsAktiviteterForAktorId(aktorId).stream()
                .map(aktivitetsoversiktWebServiceTransformer::mapTilAktivitet)
                .forEach(aktivitetsplan.getStillingaktivitetListe()::add);

        aktivitetDAO.hentEgenAktiviteterForAktorId(aktorId).stream()
                .map(aktivitetsoversiktWebServiceTransformer::mapTilAktivitet)
                .forEach(aktivitetsplan.getEgenaktivitetListe()::add);

        return wsHentAktiviteterResponse;
    }

    @Override
    public HentEndringsLoggForAktivitetResponse hentEndringsLoggForAktivitet(HentEndringsLoggForAktivitetRequest hentEndringsLoggForAktivitetRequest) {
        val endringsloggResponse = new HentEndringsLoggForAktivitetResponse();
        val endringer = endringsloggResponse.getEndringslogg();

        of(hentEndringsLoggForAktivitetRequest)
                .map(HentEndringsLoggForAktivitetRequest::getAktivitetId)
                .map(Long::parseLong)
                .map(endringsLoggDAO::hentEndringdsloggForAktivitetId)
                .ifPresent((endringslist) -> endringslist.stream()
                        .map(aktivitetsoversiktWebServiceTransformer::somEndringsLoggResponse)
                        .forEach(endringer::add)
                );
        return endringsloggResponse;
    }

    @Override
    public void ping() {
    }

    private String hentAktoerIdForIdent(String ident) {
        return aktoerConsumer.hentAktoerIdForIdent(ident)
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

}

