package no.nav.fo.veilarbaktivitet.provider;

import lombok.val;
import no.nav.fo.IntegrasjonsTestUtenArenaMock;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.fo.veilarbaktivitet.mappers.AktivitetDataMapper;
import no.nav.fo.veilarbaktivitet.mappers.AktivitetWSMapper;
import no.nav.fo.veilarbaktivitet.service.AktivitetService;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.*;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static no.nav.fo.TestData.KJENT_AKTOR_ID;
import static no.nav.fo.TestData.KJENT_IDENT;
import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.nyAktivitet;
import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.nyttStillingssøk;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.EGENAKTIVITET;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.JOBBSOEKING;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.xmlCalendar;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;


public class AktivitetsplanWSTest extends IntegrasjonsTestUtenArenaMock {

    @Test
    public void hent_aktiviteter() throws Exception {
        opprett_aktivitet();
        val req = getHentAktivitetsplanRequest();
        val res = aktivitetsplanWS.hentAktivitetsplan(req);
        assertThat(res.getAktivitetsplan().getAktivitetListe(), hasSize(1));
    }

    @Test
    public void hent_aktivitet() throws Exception {
        opprett_aktivitet();
        val req = new HentAktivitetRequest();
        val aktiviet = aktiviter().get(0);
        req.setAktivitetId(aktiviet.getId().toString());

        val res = aktivitetsplanWS.hentAktivitet(req);
        assertThat(res.getAktivitet().getAktivitetId(), equalTo(aktiviet.getId().toString()));
        assertThat(res.getAktivitet().getTittel(), equalTo(aktiviet.getTittel()));
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


        aktivitetsplanWS.opprettNyAktivitet(opprettNyAktivitetRequest);

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
        aktivitetsplanWS.slettAktivitet(slettReq);

        assertThat(aktiviter(), empty());
    }

    @Test
    public void endre_aktivitet_status() throws Exception {
        opprett_aktivitet();

        val aktivitet = aktiviter().get(0).toBuilder().status(AktivitetStatus.GJENNOMFORT).build();

        val endreReq = new EndreAktivitetStatusRequest();
        endreReq.setAktivitet(AktivitetWSMapper.mapTilAktivitet("123", aktivitet));

        val res1 = aktivitetsplanWS.endreAktivitetStatus(endreReq);
        assertThat(res1.getAktivitet().getStatus(), equalTo(Status.GJENNOMFOERT));

        val avbruttAktivitet = aktivitet.toBuilder().status(AktivitetStatus.AVBRUTT).build();
        val aktivitet2 = AktivitetWSMapper.mapTilAktivitet("123", avbruttAktivitet);
        endreReq.setAktivitet(aktivitet2);

        val res2 = aktivitetsplanWS.endreAktivitetStatus(endreReq);
        assertThat(res2.getAktivitet().getStatus(), equalTo(Status.AVBRUTT));
    }

    @Test
    public void endre_aktivitet_etikett() throws Exception {
        opprett_stilling_aktivitet();

        val aktivitet = aktiviter().get(0);
        aktivitet.getStillingsSoekAktivitetData().setStillingsoekEtikett(StillingsoekEtikettData.AVSLAG);

        val endreReq = new EndreAktivitetEtikettRequest();
        endreReq.setAktivitet(AktivitetWSMapper.mapTilAktivitet("123", aktivitet));

        val res1 = aktivitetsplanWS.endreAktivitetEtikett(endreReq);
        assertThat(res1.getAktivitet().getStillingAktivitet().getEtikett(), equalTo(Etikett.AVSLAG));
    }

    @Test
    public void hent_aktivitet_versjoner() throws Exception {
        opprett_aktivitet();
        val aktivitet = aktiviter().get(0);

        val endreReq = new EndreAktivitetStatusRequest();
        endreReq.setAktivitet(AktivitetWSMapper.mapTilAktivitet("123", aktivitet));
        aktivitetsplanWS.endreAktivitetStatus(endreReq);

        val hentVersjoner = new HentAktivitetVersjonerRequest();
        hentVersjoner.setAktivitetId(aktivitet.getId().toString());
        val versjoner = aktivitetsplanWS.hentAktivitetVersjoner(hentVersjoner).getAktivitetversjoner();
        assertThat(versjoner, hasSize(2));
        assertThat(versjoner.get(0).getVersjon(), equalTo("1"));
        assertThat(versjoner.get(1).getVersjon(), equalTo("0"));
    }

    @Test
    public void oppdater_aktivitet() throws Exception {
        opprett_aktivitet();

        val nyBeskrivelse = "batman > superman";
        val nyLenke = "www.everythingIsAwesome.com";
        val aktivitet = aktiviter()
                .get(0)
                .toBuilder()
                .beskrivelse(nyBeskrivelse)
                .lenke(nyLenke)
                .build();

        val endreReq = new EndreAktivitetRequest();
        endreReq.setAktivitet(AktivitetWSMapper.mapTilAktivitet("", aktivitet));
        val resp = aktivitetsplanWS.endreAktivitet(endreReq);

        assertThat(resp.getAktivitet().getBeskrivelse(), equalTo(nyBeskrivelse));
        assertThat(resp.getAktivitet().getLenke(), equalTo(nyLenke));
    }

    @Test
    public void skal_ikke_kunne_endre_en_avtalt_aktivitet() throws Exception {
        opprett_avtalt_aktivitet();
        val aktivitet = aktiviter().get(0);

        val endreReq = new EndreAktivitetRequest();
        val endreAktivitet = AktivitetWSMapper.mapTilAktivitet("", aktivitet);
        endreAktivitet.setTom(xmlCalendar(new Date(0)));
        endreAktivitet.setBeskrivelse("bleeeeeee123");
        endreReq.setAktivitet(endreAktivitet);

        val resp = aktivitetsplanWS.endreAktivitet(endreReq);
        val respAktivitet = AktivitetDataMapper
                .mapTilAktivitetData(resp.getAktivitet());
        assertThat(aktivitet.toBuilder()
                        .aktorId(null)
                        .transaksjonsType(null)
                        .endretDato(null)
                        .historiskDato(null)
                        .build(),
                equalTo(respAktivitet));
    }

    @Inject
    private AktivitetsplanWS aktivitetsplanWS;

    @Inject
    private AktivitetService aktivitetService;

    private List<AktivitetData> aktiviter() throws Exception {
        return aktivitetService.hentAktiviteterForAktorId(KJENT_AKTOR_ID);
    }

    private void opprett_aktivitet() {
        val aktivitet = nyAktivitet()
                .aktivitetType(EGENAKTIVITET)
                .egenAktivitetData(new EgenAktivitetData())
                .build();

        aktivitetService.opprettAktivitet(KJENT_AKTOR_ID, aktivitet);
    }

    private void opprett_stilling_aktivitet() {
        val aktivitet = nyAktivitet()
                .aktivitetType(JOBBSOEKING)
                .stillingsSoekAktivitetData(nyttStillingssøk())
                .build();

        aktivitetService.opprettAktivitet(KJENT_AKTOR_ID, aktivitet);
    }

    private void opprett_avtalt_aktivitet() {
        val aktivitet = nyAktivitet()
                .aktivitetType(JOBBSOEKING)
                .avtalt(true)
                .stillingsSoekAktivitetData(new StillingsoekAktivitetData())
                .build();

        aktivitetService.opprettAktivitet(KJENT_AKTOR_ID, aktivitet);
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