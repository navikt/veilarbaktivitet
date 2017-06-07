package no.nav.fo.veilarbaktivitet.provider;

import no.nav.fo.IntegrasjonsTestUtenArenaMock;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;


public class AktivitetsplanWSTest extends IntegrasjonsTestUtenArenaMock {
    //
    @Test
    public void hent_aktiviteter() throws Exception {
//        opprett_aktivitet();
//        val req = getHentAktivitetsplanRequest();
//        val res = aktivitetsplanWS.hentAktivitetsplan(req);
//        assertThat(res.getAktivitetsplan().getAktivitetListe(), hasSize(1));
        assertThat(null, hasSize(1));

    }
//
//    @Test
//    public void hent_aktivitet() throws Exception {
//        opprett_aktivitet();
//        val req = new HentAktivitetRequest();
//        val aktiviet = aktiviter().get(0);
//        req.setAktivitetId(aktiviet.getId().toString());
//
//        val res = aktivitetsplanWS.hentAktivitet(req);
//        assertThat(res.getAktivitet().getAktivitetId(), equalTo(aktiviet.getId().toString()));
//        assertThat(res.getAktivitet().getTittel(), equalTo(aktiviet.getTittel()));
//    }
//
//    @Test
//    public void opprett_aktiviteter() throws Exception {
//        val opprettNyAktivitetRequest = getOpprettNyAktivitetRequest();
//        val beskrivelse = "Batman er awesome!!!!!";
//        Innsender innsender = new Innsender();
//        innsender.setId("Batman");
//        innsender.setType(InnsenderType.BRUKER);
//        opprettNyAktivitetRequest.getAktivitet().setLagtInnAv(innsender);
//
//        opprettNyAktivitetRequest.getAktivitet().setBeskrivelse(beskrivelse);
//
//
//        aktivitetsplanWS.opprettNyAktivitet(opprettNyAktivitetRequest);
//
//        val aktiviter = aktiviter();
//        assertThat(aktiviter, hasSize(1));
//        assertThat(aktiviter.get(0).getBeskrivelse(), containsString(beskrivelse));
//    }
//
//    @Test
//    public void slett_aktivitet() throws Exception {
//        opprett_aktivitet();
//        val aktivitetId = Long.toString(aktiviter().get(0).getId());
//
//        assertThat(aktiviter(), hasSize(1));
//
//        val slettReq = new SlettAktivitetRequest();
//        slettReq.setAktivitetId(aktivitetId);
//        aktivitetsplanWS.slettAktivitet(slettReq);
//
//        assertThat(aktiviter(), empty());
//    }
//
//    @Test
//    public void endre_aktivitet_status() throws Exception {
//        opprett_aktivitet();
//
//        val aktivitet = aktiviter().get(0);
//        aktivitet.setStatus(AktivitetStatus.GJENNOMFORT);
//
//        val endreReq = new EndreAktivitetStatusRequest();
//        endreReq.setAktivitet(SoapMapper.mapTilAktivitet("123", aktivitet));
//
//        val res1 = aktivitetsplanWS.endreAktivitetStatus(endreReq);
//        assertThat(res1.getAktivitet().getStatus(), equalTo(Status.GJENNOMFOERT));
//
//        aktivitet.setStatus(AktivitetStatus.AVBRUTT);
//        endreReq.setAktivitet(SoapMapper.mapTilAktivitet("", aktivitet));
//        val res2 = aktivitetsplanWS.endreAktivitetStatus(endreReq);
//        assertThat(res2.getAktivitet().getStatus(), equalTo(Status.AVBRUTT));
//    }
//
//    @Test
//    public void endre_aktivitet_etikett() throws Exception {
//        opprett_stilling_aktivitet();
//
//        val aktivitet = aktiviter().get(0);
//        aktivitet.getStillingsSoekAktivitetData().setStillingsoekEtikett(StillingsoekEtikettData.AVSLAG);
//
//        val endreReq = new EndreAktivitetEtikettRequest();
//        endreReq.setAktivitet(SoapMapper.mapTilAktivitet("123", aktivitet));
//
//        val res1 = aktivitetsplanWS.endreAktivitetEtikett(endreReq);
//        assertThat(res1.getAktivitet().getStillingAktivitet().getEtikett(), equalTo(Etikett.AVSLAG));
//    }
//
//    @Test
//    public void hent_endringslogg() throws Exception {
//        opprett_aktivitet();
//
//        val aktivitet = aktiviter().get(0);
//        aktivitet.setStatus(AktivitetStatus.GJENNOMFORT);
//
//        val endreReq = new EndreAktivitetStatusRequest();
//        endreReq.setAktivitet(SoapMapper.mapTilAktivitet("", aktivitet));
//
//        aktivitetsplanWS.endreAktivitetStatus(endreReq);
//
//        val req = new HentEndringsLoggForAktivitetRequest();
//        req.setAktivitetId(Long.toString(aktivitet.getId()));
//        assertThat(aktivitetsplanWS.hentEndringsLoggForAktivitet(req).getEndringslogg(), hasSize(1));
//    }
//
//    @Test
//    public void oppdater_aktivitet() throws Exception {
//        opprett_aktivitet();
//        val aktivitet = aktiviter().get(0);
//
//        val nyBeskrivelse = "batman > superman";
//        val nyLenke = "www.everythingIsAwesome.com";
//        aktivitet.setBeskrivelse(nyBeskrivelse);
//        aktivitet.setLenke(nyLenke);
//
//        val endreReq = new EndreAktivitetRequest();
//        endreReq.setAktivitet(SoapMapper.mapTilAktivitet("", aktivitet));
//        val resp = aktivitetsplanWS.endreAktivitet(endreReq);
//
//        assertThat(resp.getAktivitet().getBeskrivelse(), equalTo(nyBeskrivelse));
//        assertThat(resp.getAktivitet().getLenke(), equalTo(nyLenke));
//    }
//
//    @Test
//    public void skal_ikke_kunne_endre_en_avtalt_aktivitet() throws Exception {
//        opprett_avtalt_aktivitet();
//        val aktivitet = aktiviter().get(0);
//
//        val endreReq = new EndreAktivitetRequest();
//        val endreAktivitet = SoapMapper.mapTilAktivitet("", aktivitet);
//        endreAktivitet.setTom(xmlCalendar(new Date()));
//        endreAktivitet.setBeskrivelse("bleeeeeee123");
//        endreReq.setAktivitet(endreAktivitet);
//
//        val resp = aktivitetsplanWS.endreAktivitet(endreReq);
//        val respAktivitet = SoapMapper.mapTilAktivitetData(resp.getAktivitet());
//        assertThat(respAktivitet, equalTo(aktivitet.setAktorId(respAktivitet.getAktorId())));
//    }
//
//
//
//    @Inject
//    private AktivitetsplanWS aktivitetsplanWS;
//
//    @Inject
//    private AktivitetDAO aktivitetDAO;
//
//    private List<AktivitetData> aktiviter() throws Exception {
//        return aktivitetDAO.hentAktiviteterForAktorId(KJENT_AKTOR_ID);
//    }
//
//    private void opprett_aktivitet() {
//        val aktivitet = nyAktivitet(KJENT_AKTOR_ID)
//                .setAktivitetType(EGENAKTIVITET)
//                .setEgenAktivitetData(new EgenAktivitetData());
//
//        aktivitetDAO.opprettAktivitet(aktivitet);
//    }
//
//    private void opprett_stilling_aktivitet() {
//        val aktivitet = nyAktivitet(KJENT_AKTOR_ID)
//                .setAktivitetType(JOBBSOEKING)
//                .setStillingsSoekAktivitetData(nyttStillingssøk());
//
//        aktivitetDAO.opprettAktivitet(aktivitet);
//    }
//
//    private void opprett_avtalt_aktivitet() {
//        val aktivitet = nyAktivitet(KJENT_AKTOR_ID)
//                .setAktivitetType(AktivitetTypeData.JOBBSOEKING)
//                .setAvtalt(true)
//                .setStillingsSoekAktivitetData(new StillingsoekAktivitetData());
//
//        aktivitetDAO.opprettAktivitet(aktivitet);
//    }
//
//    private OpprettNyAktivitetRequest getOpprettNyAktivitetRequest() {
//        OpprettNyAktivitetRequest opprettNyAktivitetRequest = new OpprettNyAktivitetRequest();
//
//        val aktivitet = nyAktivitetWS();
//        aktivitet.setEgenAktivitet(new Egenaktivitet());
//
//        opprettNyAktivitetRequest.setAktivitet(aktivitet);
//        return opprettNyAktivitetRequest;
//    }
//
//    private HentAktivitetsplanRequest getHentAktivitetsplanRequest() {
//        val hentAktiviteterRequest = new HentAktivitetsplanRequest();
//        hentAktiviteterRequest.setPersonident(KJENT_IDENT);
//        return hentAktiviteterRequest;
//    }
//
//    private Aktivitet nyAktivitetWS() {
//        Aktivitet aktivitet = new Aktivitet();
//        aktivitet.setPersonIdent(KJENT_IDENT);
//        aktivitet.setStatus(Status.values()[0]);
//        aktivitet.setType(AktivitetType.EGENAKTIVITET);
//        return aktivitet;
//    }

}