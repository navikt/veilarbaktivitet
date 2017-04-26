package no.nav.fo.veilarbaktivitet.ws.provider;

import lombok.val;
import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarbaktivitet.db.dao.AktivitetDAO;
import no.nav.fo.veilarbaktivitet.domain.AktivitetData;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.EgenAktivitetData;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.*;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static no.nav.fo.TestData.KJENT_AKTOR_ID;
import static no.nav.fo.TestData.KJENT_IDENT;
import static no.nav.fo.veilarbaktivitet.AktivitetDataBuilder.nyAktivitet;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.EGENAKTIVITET;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.xmlCalendar;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;


public class SoapServiceTest extends IntegrasjonsTest {

    @Test
    public void hent_aktiviteter() throws Exception {
        val hentAktiviteterRequest = getHentAktivitetsplanRequest();
        opprett_aktivitet();

        val hentAktiviteterResponse2 = soapService.hentAktivitetsplan(hentAktiviteterRequest);
        assertThat(hentAktiviteterResponse2.getAktivitetsplan().getAktivitetListe(), hasSize(1));
    }

    @Test
    public void opprett_aktiviteter() throws Exception {
        val opprettNyAktivitetRequest = getOpprettNyAktivitetRequest();
        val beskrivelse = "Batman er awesome!!!!!";
        Innsender innsender = new Innsender();
        innsender.setId("Batman");
        innsender.setType(InnsenderType.BRUKER);
        opprettNyAktivitetRequest.getAktivitet().setLagtInnAv(innsender);

        opprettNyAktivitetRequest.getAktivitet().setBeskrivelse(beskrivelse);


        soapService.opprettNyAktivitet(opprettNyAktivitetRequest);

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
        soapService.slettAktivitet(slettReq);

        assertThat(aktiviter(), empty());
    }

    @Test
    public void endre_aktivitet_status() throws Exception {
        opprett_aktivitet();

        val aktivitet = aktiviter().get(0);
        aktivitet.setStatus(AktivitetStatus.GJENNOMFORT);

        val endreReq = new EndreAktivitetStatusRequest();
        endreReq.setAktivitet(SoapServiceMapper.mapTilAktivitet("123", aktivitet));

        val res1 = soapService.endreAktivitetStatus(endreReq);
        assertThat(res1.getAktivitet().getStatus(), equalTo(Status.GJENNOMFOERT));

        aktivitet.setStatus(AktivitetStatus.AVBRUTT);
        endreReq.setAktivitet(SoapServiceMapper.mapTilAktivitet("", aktivitet));
        val res2 = soapService.endreAktivitetStatus(endreReq);
        assertThat(res2.getAktivitet().getStatus(), equalTo(Status.AVBRUTT));
    }

    @Test
    public void hent_endringslogg() throws Exception {
        opprett_aktivitet();

        val aktivitet = aktiviter().get(0);
        aktivitet.setStatus(AktivitetStatus.GJENNOMFORT);

        val endreReq = new EndreAktivitetStatusRequest();
        endreReq.setAktivitet(SoapServiceMapper.mapTilAktivitet("", aktivitet));

        soapService.endreAktivitetStatus(endreReq);

        val req = new HentEndringsLoggForAktivitetRequest();
        req.setAktivitetId(Long.toString(aktivitet.getId()));
        assertThat(soapService.hentEndringsLoggForAktivitet(req).getEndringslogg(), hasSize(1));
    }

    @Test
    public void oppdater_aktivitet() throws Exception {
        opprett_aktivitet();
        val aktivitet = aktiviter().get(0);

        val nyBeskrivelse = "batman > superman";
        val nyLenke = "www.everythingIsAwesome.com";
        aktivitet.setBeskrivelse(nyBeskrivelse);
        aktivitet.setLenke(nyLenke);

        val endreReq = new EndreAktivitetRequest();
        endreReq.setAktivitet(SoapServiceMapper.mapTilAktivitet("", aktivitet));
        val resp = soapService.endreAktivitet(endreReq);

        assertThat(resp.getAktivitet().getBeskrivelse(), equalTo(nyBeskrivelse));
        assertThat(resp.getAktivitet().getLenke(), equalTo(nyLenke));
    }

    @Test
    public void skal_ikke_kunne_endre_en_avtalt_aktivitet() throws Exception {
        opprett_avtalt_aktivitet();
        val aktivitet = aktiviter().get(0);

        val endreReq = new EndreAktivitetRequest();
        val endreAktivitet = SoapServiceMapper.mapTilAktivitet("", aktivitet);
        endreAktivitet.setTom(xmlCalendar(new Date()));
        endreAktivitet.setBeskrivelse("bleeeeeee123");
        endreReq.setAktivitet(endreAktivitet);

        val resp = soapService.endreAktivitet(endreReq);
        val respAktivitet = SoapServiceMapper.mapTilAktivitetData(resp.getAktivitet());
        assertThat(respAktivitet, equalTo(aktivitet.setAktorId(respAktivitet.getAktorId())));
    }


    @Inject
    private SoapService soapService;

    @Inject
    private AktivitetDAO aktivitetDAO;

    private List<AktivitetData> aktiviter() throws Exception {
        return aktivitetDAO.hentAktiviteterForAktorId(KJENT_AKTOR_ID);
    }

    private void opprett_aktivitet() {
        val aktivitet = nyAktivitet(KJENT_AKTOR_ID)
                .setAktivitetType(EGENAKTIVITET)
                .setEgenAktivitetData(new EgenAktivitetData());

        aktivitetDAO.opprettAktivitet(aktivitet);
    }

    private void opprett_avtalt_aktivitet() {
        val aktivitet = nyAktivitet(KJENT_AKTOR_ID)
                .setAktivitetType(EGENAKTIVITET)
                .setAvtalt(true)
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