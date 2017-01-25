package no.nav.fo.veilarbaktivitet.ws.provider;

import no.nav.fo.veilarbaktivitet.db.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.Aktivitet;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.fo.veilarbaktivitet.domain.AktivitetType;
import no.nav.fo.veilarbaktivitet.domain.Innsender;
import no.nav.fo.veilarbaktivitet.ws.consumer.AktoerConsumer;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.BehandleAktivitetsplanV1;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.HentAktivitetsplanSikkerhetsbegrensing;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.OpprettNyStillingAktivitetSikkerhetsbegrensing;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.binding.OpprettNyStillingAktivitetUgyldigInput;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.*;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.jws.WebService;

import static java.util.Optional.of;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetStatus.*;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetType.EGENAKTIVITET;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetType.JOBBSØKING;
import static no.nav.fo.veilarbaktivitet.domain.Innsender.BRUKER;
import static no.nav.fo.veilarbaktivitet.domain.Innsender.NAV;

@WebService
@Service
public class AktivitetsoversiktWebService implements BehandleAktivitetsplanV1 {

    @Inject
    private AktoerConsumer aktoerConsumer;

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Inject
    private AktivitetsoversiktWebServiceTransformer aktivitetsoversiktWebServiceTransformer;

    @Override
    public OpprettNyEgenAktivitetResponse opprettNyEgenAktivitet(OpprettNyEgenAktivitetRequest opprettNyEgenAktivitetRequest) {
        return of(opprettNyEgenAktivitetRequest)
                .map(aktivitetsoversiktWebServiceTransformer::somEgenAktivitet)
                .map(aktivitetDAO::opprettEgenAktivitet)
                .map(aktivitetsoversiktWebServiceTransformer::somWSAktivitet)
                .map(aktivitetsoversiktWebServiceTransformer::somOpprettNyEgenAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public OpprettNyStillingAktivitetResponse opprettNyStillingAktivitet(OpprettNyStillingAktivitetRequest opprettNyStillingAktivitetRequest) throws OpprettNyStillingAktivitetSikkerhetsbegrensing, OpprettNyStillingAktivitetUgyldigInput {
        return of(opprettNyStillingAktivitetRequest)
                .map(aktivitetsoversiktWebServiceTransformer::somStillingAktivitet)
                .map(aktivitetDAO::opprettStillingAktivitet)
                .map(aktivitetsoversiktWebServiceTransformer::somWSAktivitet)
                .map(aktivitetsoversiktWebServiceTransformer::somOpprettNyStillingAktivitetResponse)
                .orElseThrow(RuntimeException::new);
    }

    @Override
    public HentAktivitetsplanResponse hentAktivitetsplan(HentAktivitetsplanRequest hentAktivitetsplanRequest) throws HentAktivitetsplanSikkerhetsbegrensing {
        String aktorId = hentAktoerIdForIdent(hentAktivitetsplanRequest.getPersonident());

        HentAktivitetsplanResponse wsHentAktiviteterResponse = new HentAktivitetsplanResponse();
        Aktivitetsplan aktivitetsplan = new Aktivitetsplan();
        wsHentAktiviteterResponse.setAktivitetsplan(aktivitetsplan);
        aktivitetDAO.hentStillingsAktiviteterForAktorId(aktorId).stream().map(aktivitetsoversiktWebServiceTransformer::somWSAktivitet).forEach(aktivitetsplan.getStillingaktivitetListe()::add);
        aktivitetDAO.hentEgenAktiviteterForAktorId(aktorId).stream().map(aktivitetsoversiktWebServiceTransformer::somWSAktivitet).forEach(aktivitetsplan.getEgenaktivitetListe()::add);
        return wsHentAktiviteterResponse;
    }

    @Override
    public void ping() {
    }

    private String hentAktoerIdForIdent(String ident) {
        return aktoerConsumer.hentAktoerIdForIdent(ident)
                .orElseThrow(RuntimeException::new); // Hvordan håndere dette?
    }

}

