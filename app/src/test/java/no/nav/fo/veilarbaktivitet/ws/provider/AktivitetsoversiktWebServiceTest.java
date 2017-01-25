package no.nav.fo.veilarbaktivitet.ws.provider;

import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.TestData;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.HentAktivitetsplanRequest;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.HentAktivitetsplanResponse;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.OpprettNyEgenAktivitetRequest;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.OpprettNyStillingAktivitetRequest;
import org.junit.Test;

import javax.inject.Inject;

import static no.nav.fo.TestData.KJENT_IDENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;


public class AktivitetsoversiktWebServiceTest extends IntegrasjonsTest {

    @Inject
    private AktivitetsoversiktWebService aktivitetsoversiktWebService;

    @Test
    public void hent_og_opprett_aktiviteter() throws Exception {
        HentAktivitetsplanRequest hentAktiviteterRequest = new HentAktivitetsplanRequest();
        hentAktiviteterRequest.setPersonident(KJENT_IDENT);

        HentAktivitetsplanResponse hentAktiviteterResponse = aktivitetsoversiktWebService.hentAktivitetsplan(hentAktiviteterRequest);
        Aktivitetsplan aktivitetsplan = hentAktiviteterResponse.getAktivitetsplan();
        assertThat(aktivitetsplan.getEgenaktivitetListe(), empty());
        assertThat(aktivitetsplan.getStillingaktivitetListe(), empty());

        OpprettNyEgenAktivitetRequest opprettNyEgenAktivitetRequest = new OpprettNyEgenAktivitetRequest();

        Egenaktivitet egenaktivitet = new Egenaktivitet();
        egenaktivitet.setAktivitet(nyAktivitet());
        opprettNyEgenAktivitetRequest.setEgenaktivitet( egenaktivitet);
        aktivitetsoversiktWebService.opprettNyEgenAktivitet(opprettNyEgenAktivitetRequest);

        OpprettNyStillingAktivitetRequest opprettNyStillingAktivitetRequest = new OpprettNyStillingAktivitetRequest();
        Stillingaktivitet stillingaktivitet = new Stillingaktivitet();
        stillingaktivitet.setAktivitet(nyAktivitet());
        opprettNyStillingAktivitetRequest.setStillingaktivitet(stillingaktivitet);
        aktivitetsoversiktWebService.opprettNyStillingAktivitet(opprettNyStillingAktivitetRequest);

        HentAktivitetsplanResponse wsHentAktiviteterResponse2 = aktivitetsoversiktWebService.hentAktivitetsplan(hentAktiviteterRequest);
        Aktivitetsplan aktivitetsplan2 = wsHentAktiviteterResponse2.getAktivitetsplan();
        assertThat(aktivitetsplan2.getEgenaktivitetListe(), hasSize(1));
        assertThat(aktivitetsplan2.getStillingaktivitetListe(), hasSize(1));
    }

    private Aktivitet nyAktivitet() {
        Aktivitet aktivitet = new Aktivitet();
        aktivitet.setPersonIdent(KJENT_IDENT);
        aktivitet.setStatus(Status.values()[0]);
        return aktivitet;
    }

}