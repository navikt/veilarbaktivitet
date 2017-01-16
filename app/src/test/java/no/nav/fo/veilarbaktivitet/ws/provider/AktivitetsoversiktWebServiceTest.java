package no.nav.fo.veilarbaktivitet.ws.provider;

import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.TestData;
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
    public void hent_og_opprett_aktiviteter() {
        AktivitetsoversiktWebService.WSHentAktiviteterRequest hentAktiviteterRequest = new AktivitetsoversiktWebService.WSHentAktiviteterRequest();
        hentAktiviteterRequest.ident = KJENT_IDENT;

        AktivitetsoversiktWebService.WSHentAktiviteterResponse hentAktiviteterResponse = aktivitetsoversiktWebService.hentAktiviteter(hentAktiviteterRequest);
        assertThat(hentAktiviteterResponse.aktivitetsoversikt.egenAktiviteter, empty());
        assertThat(hentAktiviteterResponse.aktivitetsoversikt.stillingsAktiviteter, empty());

        AktivitetsoversiktWebService.WSOpprettNyEgenAktivitetRequest opprettNyEgenAktivitetRequest = new AktivitetsoversiktWebService.WSOpprettNyEgenAktivitetRequest();
        opprettNyEgenAktivitetRequest.ident = KJENT_IDENT;
        aktivitetsoversiktWebService.opprettNyEgenAktivitet(opprettNyEgenAktivitetRequest);

        AktivitetsoversiktWebService.WSOpprettNyStillingAktivitetRequest opprettNyStillingAktivitetRequest = new AktivitetsoversiktWebService.WSOpprettNyStillingAktivitetRequest();
        opprettNyStillingAktivitetRequest.ident = KJENT_IDENT;
        aktivitetsoversiktWebService.opprettNyStillingAktivitet(opprettNyStillingAktivitetRequest);

        AktivitetsoversiktWebService.WSHentAktiviteterResponse wsHentAktiviteterResponse2 = aktivitetsoversiktWebService.hentAktiviteter(hentAktiviteterRequest);
        assertThat(wsHentAktiviteterResponse2.aktivitetsoversikt.egenAktiviteter, hasSize(1));
        assertThat(wsHentAktiviteterResponse2.aktivitetsoversikt.stillingsAktiviteter, hasSize(1));
    }

}