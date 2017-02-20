package no.nav.fo.veilarbaktivitet.ws.provider;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.TestData;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.HentAktivitetsplanRequest;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.HentAktivitetsplanResponse;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.OpprettNyAktivitetRequest;
import org.junit.Test;

import javax.inject.Inject;

import static no.nav.fo.TestData.KJENT_IDENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;


public class AktivitetsoversiktWebServiceTest extends IntegrasjonsTest {

    @Inject
    private AktivitetsoversiktWebService aktivitetsoversiktWebService;

    @Test
    public void hent_aktiviteter() throws Exception {
        HentAktivitetsplanRequest hentAktiviteterRequest = getHentAktivitetsplanRequest();

        val hentAktiviteterResponse = aktivitetsoversiktWebService.hentAktivitetsplan(hentAktiviteterRequest);
        val aktivitetsplan = hentAktiviteterResponse.getAktivitetsplan();

        assertThat(aktivitetsplan.getAktivitetListe(), empty());
    }

    @Test
    public void opprett_aktiviteter() throws Exception {
        val hentAktiviteterRequest = getHentAktivitetsplanRequest();

        OpprettNyAktivitetRequest opprettNyAktivitetRequest = new OpprettNyAktivitetRequest();

        val aktivitet = nyAktivitet();
        val beskrivelse = "Batman er awesome!!!!!";
        aktivitet.setBeskrivelse(beskrivelse);
        aktivitet.setEgenAktivitet(new Egenaktivitet());

        opprettNyAktivitetRequest.setAktivitet(aktivitet);
        aktivitetsoversiktWebService.opprettNyAktivitet(opprettNyAktivitetRequest);


        val wsHentAktiviteterResponse = aktivitetsoversiktWebService.hentAktivitetsplan(hentAktiviteterRequest);
        val aktivitetsplan = wsHentAktiviteterResponse.getAktivitetsplan();

        val aktiviter =  aktivitetsplan.getAktivitetListe();
        assertThat(aktiviter, hasSize(1));
        assertThat(aktiviter.get(0).getBeskrivelse(), containsString(beskrivelse));
    }

    private HentAktivitetsplanRequest getHentAktivitetsplanRequest() {
        val hentAktiviteterRequest = new HentAktivitetsplanRequest();
        hentAktiviteterRequest.setPersonident(KJENT_IDENT);
        return hentAktiviteterRequest;
    }

    private Aktivitet nyAktivitet() {
        Aktivitet aktivitet = new Aktivitet();
        aktivitet.setPersonIdent(KJENT_IDENT);
        aktivitet.setStatus(Status.values()[0]);
        aktivitet.setType(AktivitetType.EGENAKTIVITET);
        return aktivitet;
    }

}