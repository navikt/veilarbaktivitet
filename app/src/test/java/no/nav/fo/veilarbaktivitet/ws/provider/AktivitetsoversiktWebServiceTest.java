package no.nav.fo.veilarbaktivitet.ws.provider;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbaktivitet.db.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatusData;
import no.nav.fo.veilarbaktivitet.domain.EgenAktivitetData;
import no.nav.fo.veilarbaktivitet.ws.consumer.AktoerConsumer;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Aktivitet;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.AktivitetType;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Egenaktivitet;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.Status;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static no.nav.fo.TestData.KJENT_AKTOR_ID;
import static no.nav.fo.TestData.KJENT_IDENT;
import static no.nav.fo.veilarbaktivitet.AktivitetDataHelper.nyAktivitet;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.EGENAKTIVITET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.when;


public class AktivitetsoversiktWebServiceTest extends IntegrasjonsTest {

    @Inject
    private AktivitetsoversiktWebService aktivitetsoversiktWebService;

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Before
    public void setup() {
        val aktoerConsumer = aktivitetsoversiktWebService.aktoerConsumer = Mockito.mock(AktoerConsumer.class);
        when(aktoerConsumer.hentAktoerIdForIdent(KJENT_IDENT)).thenReturn(Optional.of(KJENT_AKTOR_ID));
    }

    @Test
    public void hent_aktiviteter() throws Exception {
        HentAktivitetsplanRequest hentAktiviteterRequest = getHentAktivitetsplanRequest();

        val hentAktiviteterResponse1 = aktivitetsoversiktWebService.hentAktivitetsplan(hentAktiviteterRequest);
        assertThat(hentAktiviteterResponse1.getAktivitetsplan().getAktivitetListe(), empty());

        opprett_aktivitet();
        val hentAktiviteterResponse2 = aktivitetsoversiktWebService.hentAktivitetsplan(hentAktiviteterRequest);
        assertThat(hentAktiviteterResponse2.getAktivitetsplan().getAktivitetListe(), hasSize(1));
    }

    @Test
    public void opprett_aktiviteter() throws Exception {
        val opprettNyAktivitetRequest = getOpprettNyAktivitetRequest();
        val beskrivelse = "Batman er awesome!!!!!";
        opprettNyAktivitetRequest.getAktivitet().setBeskrivelse(beskrivelse);
        aktivitetsoversiktWebService.opprettNyAktivitet(opprettNyAktivitetRequest);

        val aktiviter = aktiviter();
        assertThat(aktiviter, hasSize(1));
        assertThat(aktiviter.get(0).getBeskrivelse(), containsString(beskrivelse));
    }

    @Test
    public void slett_aktivitet() throws Exception {
        opprett_aktivitet();
        val aktivitetId = Long.toString(aktiviter().get(0).getId());

        assertThat(aktiviter(), hasSize(1));

        val slettReq = new SlettAktivitetRequest();
        slettReq.setAktivitetId(aktivitetId);
        aktivitetsoversiktWebService.slettAktivitet(slettReq);

        assertThat(aktiviter(), empty());
    }

    @Test
    public void endre_aktivitet_status() throws Exception {
        opprett_aktivitet();

        val aktivitetId = Long.toString(aktiviter().get(0).getId());
        val endreReq = new EndreAktivitetStatusRequest();
        endreReq.setAktivitetId(aktivitetId);
        endreReq.setStatus(Status.GJENNOMFOERT);

        val res1 = aktivitetsoversiktWebService.endreAktivitetStatus(endreReq);
        assertThat(res1.getAktivitet().getStatus(), equalTo(Status.GJENNOMFOERT));


        endreReq.setStatus(Status.AVBRUTT);
        val res2 = aktivitetsoversiktWebService.endreAktivitetStatus(endreReq);
        assertThat(res2.getAktivitet().getStatus(), equalTo(Status.AVBRUTT));
    }

    @Test
    public void skal_ikke_kunne_endre_aktivitet_status_fra_avbrutt_eller_fullfort() throws Exception {
        val aktivitet = nyAktivitet(KJENT_AKTOR_ID)
                .setAktivitetType(EGENAKTIVITET)
                .setEgenAktivitetData(new EgenAktivitetData())
                .setStatus(AktivitetStatusData.AVBRUTT);

        aktivitetDAO.opprettAktivitet(aktivitet);

        val aktivitetId = Long.toString(aktiviter().get(0).getId());
        val endreReq = new EndreAktivitetStatusRequest();
        endreReq.setAktivitetId(aktivitetId);
        endreReq.setStatus(Status.GJENNOMFOERT);

        val res1 = aktivitetsoversiktWebService.endreAktivitetStatus(endreReq);
        assertThat(res1.getAktivitet().getStatus(), equalTo(Status.AVBRUTT));
    }

    @Test
    public void hent_endringslogg() throws Exception {
        opprett_aktivitet();

        val aktivitetId = Long.toString(aktiviter().get(0).getId());
        val endreReq = new EndreAktivitetStatusRequest();
        endreReq.setAktivitetId(aktivitetId);
        endreReq.setStatus(Status.GJENNOMFOERT);

        aktivitetsoversiktWebService.endreAktivitetStatus(endreReq);

        val req = new HentEndringsLoggForAktivitetRequest();
        req.setAktivitetId(aktivitetId);
        assertThat(aktivitetsoversiktWebService.hentEndringsLoggForAktivitet(req).getEndringslogg(), hasSize(1));
    }

    private List<AktivitetData> aktiviter() throws Exception {
        return aktivitetDAO.hentAktiviteterForAktorId(KJENT_AKTOR_ID);
    }

    private void opprett_aktivitet() {
        val aktivitet = nyAktivitet(KJENT_AKTOR_ID)
                .setAktivitetType(EGENAKTIVITET)
                .setEgenAktivitetData(new EgenAktivitetData());

        aktivitetDAO.opprettAktivitet(aktivitet);
    }

    private OpprettNyAktivitetRequest getOpprettNyAktivitetRequest() {
        OpprettNyAktivitetRequest opprettNyAktivitetRequest = new OpprettNyAktivitetRequest();

        val aktivitet = nyAktivitetWS();
        aktivitet.setEgenAktivitet(new Egenaktivitet());

        opprettNyAktivitetRequest.setAktivitet(aktivitet);
        return opprettNyAktivitetRequest;
    }

    private HentAktivitetsplanRequest getHentAktivitetsplanRequest() {
        val hentAktiviteterRequest = new HentAktivitetsplanRequest();
        hentAktiviteterRequest.setPersonident(KJENT_IDENT);
        return hentAktiviteterRequest;
    }

    private Aktivitet nyAktivitetWS() {
        Aktivitet aktivitet = new Aktivitet();
        aktivitet.setPersonIdent(KJENT_IDENT);
        aktivitet.setStatus(Status.values()[0]);
        aktivitet.setType(AktivitetType.EGENAKTIVITET);
        return aktivitet;
    }

}