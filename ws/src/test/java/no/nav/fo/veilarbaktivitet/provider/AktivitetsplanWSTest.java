package no.nav.fo.veilarbaktivitet.provider;

import lombok.val;
import no.nav.apiapp.feil.UgyldigRequest;
import no.nav.brukerdialog.security.context.SubjectExtension;
import no.nav.common.auth.Subject;
import no.nav.fo.IntegrasjonsTestMedPepOgBrukerServiceMock;
import no.nav.fo.veilarbaktivitet.domain.*;
import no.nav.fo.veilarbaktivitet.mappers.AktivitetWSMapper;
import no.nav.fo.veilarbaktivitet.service.AktivitetService;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.informasjon.*;
import no.nav.tjeneste.domene.brukerdialog.behandleaktivitetsplan.v1.meldinger.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static no.nav.brukerdialog.security.domain.IdentType.EksternBruker;
import static no.nav.common.auth.SsoToken.saml;
import static no.nav.fo.TestData.KJENT_AKTOR_ID;
import static no.nav.fo.TestData.KJENT_IDENT;
import static no.nav.fo.veilarbaktivitet.AktivitetDataTestBuilder.*;
import static no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData.*;
import static no.nav.fo.veilarbaktivitet.domain.StillingsoekEtikettData.AVSLAG;
import static no.nav.fo.veilarbaktivitet.util.DateUtils.xmlCalendar;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;


@ExtendWith(SubjectExtension.class)
public class AktivitetsplanWSTest extends IntegrasjonsTestMedPepOgBrukerServiceMock {

    @BeforeEach
    public void setup(SubjectExtension.SubjectStore subjectStore) {
        subjectStore.setSubject(new Subject("testident", EksternBruker, saml("token")));
    }

    @Nested
    public class hentAktivitetsplan {
        @Test
        public void skal_hente_alle_aktiviteter() throws Exception {
            opprett_aktivitet();
            val req = getHentAktivitetsplanRequest();
            val res = aktivitetsplanWS.hentAktivitetsplan(req);
            assertThat(res.getAktivitetsplan().getAktivitetListe(), hasSize(1));
        }
    }

    @Nested
    public class hentAktivitet {
        @Test
        public void skal_hente_en_aktivitet() throws Exception {
            opprett_aktivitet();
            val req = new HentAktivitetRequest();
            val aktivitet = aktiviter().get(0);
            req.setAktivitetId(aktivitet.getId().toString());

            val res = aktivitetsplanWS.hentAktivitet(req);
            assertThat(res.getAktivitet().getAktivitetId(), equalTo(aktivitet.getId().toString()));
            assertThat(res.getAktivitet().getTittel(), equalTo(aktivitet.getTittel()));
        }
    }

    @Nested
    public class opprettAktivitet {
        @Test
        public void skal_opprette_en_aktivtet() throws Exception {
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
    }

    @Nested
    public class slettAktivitet {
        @Test
        public void skal_kunne_slette_en_aktivitet() throws Exception {
            opprett_aktivitet();
            val aktivitetId = Long.toString(aktiviter().get(0).getId());

            assertThat(aktiviter(), hasSize(1));

            val slettReq = new SlettAktivitetRequest();
            slettReq.setAktivitetId(aktivitetId);
            aktivitetsplanWS.slettAktivitet(slettReq);

            assertThat(aktiviter(), empty());
        }
    }


    @Nested
    public class endreAktivitetStatus {
        @Test
        public void skal_endre_aktivitet_status() throws Exception {
            opprett_aktivitet();

            val aktivitet = aktiviter().get(0).toBuilder().status(AktivitetStatus.GJENNOMFORES).build();

            val endreReq = new EndreAktivitetStatusRequest();
            endreReq.setAktivitet(AktivitetWSMapper.mapTilAktivitet(Person.fnr("123"), aktivitet));

            val res1 = aktivitetsplanWS.endreAktivitetStatus(endreReq);
            Aktivitet res1Aktivitet = res1.getAktivitet();
            assertThat(res1Aktivitet.getStatus(), equalTo(Status.GJENNOMFOERT));

            val avbruttAktivitet = aktivitet.withStatus(AktivitetStatus.AVBRUTT).withVersjon(Long.parseLong(res1Aktivitet.getVersjon()));
            val aktivitet2 = AktivitetWSMapper.mapTilAktivitet(Person.fnr("123"), avbruttAktivitet);
            endreReq.setAktivitet(aktivitet2);

            val res2 = aktivitetsplanWS.endreAktivitetStatus(endreReq);
            assertThat(res2.getAktivitet().getStatus(), equalTo(Status.AVBRUTT));
        }

        @Test
        public void skal_ikke_endre_status_pa_samtale_aktiviteter() {
            val samtalsAktivitet = opprett_aktivitet_med_type_og_status(SAMTALEREFERAT, AktivitetStatus.PLANLAGT);

            val endreReq = new EndreAktivitetStatusRequest();
            samtalsAktivitet.withStatus(AktivitetStatus.GJENNOMFORES);
            endreReq.setAktivitet(AktivitetWSMapper.mapTilAktivitet(Person.fnr("123"), samtalsAktivitet));

            Assertions.assertThrows(UgyldigRequest.class, () -> aktivitetsplanWS.endreAktivitetStatus(endreReq));
        }

        @Test
        public void skal_ikke_endre_status_pa_mote_aktiviteter() {
            val samtalsAktivitet = opprett_aktivitet_med_type_og_status(MOTE, AktivitetStatus.GJENNOMFORES);

            val endreReq = new EndreAktivitetStatusRequest();
            samtalsAktivitet.withStatus(AktivitetStatus.FULLFORT);
            endreReq.setAktivitet(AktivitetWSMapper.mapTilAktivitet(Person.fnr("123"), samtalsAktivitet));

            Assertions.assertThrows(UgyldigRequest.class, () -> aktivitetsplanWS.endreAktivitetStatus(endreReq));
        }

    }

    @Nested
    public class endreEtikett {
        @Test
        public void skal_endre_aktivitet_etikett() throws Exception {
            val aktivitet = opprett_stilling_aktivitet();

            val aktivitetMedAvslagEtikett = aktivitet.withStillingsSoekAktivitetData(
                    aktivitet.getStillingsSoekAktivitetData().withStillingsoekEtikett(AVSLAG)
            );

            val endreReq = new EndreAktivitetEtikettRequest();
            endreReq.setAktivitet(AktivitetWSMapper.mapTilAktivitet(Person.fnr("123"), aktivitetMedAvslagEtikett));

            val res1 = aktivitetsplanWS.endreAktivitetEtikett(endreReq);
            assertThat(res1.getAktivitet().getStillingAktivitet().getEtikett(), equalTo(Etikett.AVSLAG));
        }
    }


    @Nested
    public class hentAktivitetVersjoner {
        @Test
        public void skal_hente_aktivitet_versjoner() throws Exception {
            opprett_aktivitet();
            val aktivitet = aktiviter().get(0);

            val endreReq = new EndreAktivitetStatusRequest();
            endreReq.setAktivitet(AktivitetWSMapper.mapTilAktivitet(Person.fnr("123"), aktivitet));
            aktivitetsplanWS.endreAktivitetStatus(endreReq);

            val hentVersjoner = new HentAktivitetVersjonerRequest();
            hentVersjoner.setAktivitetId(aktivitet.getId().toString());
            val versjoner = aktivitetsplanWS.hentAktivitetVersjoner(hentVersjoner).getAktivitetversjoner();
            assertThat(versjoner, hasSize(2));
            String versjon1 = versjoner.get(0).getVersjon();
            String versjon2 = versjoner.get(1).getVersjon();
            assertThat(versjon1, not(equalTo(versjon2)));
        }

        @Test
        public void skal_ikke_hent_interne_aktivitet_versjoner() throws Exception {
            val aktivitet = opprett_mote_aktivitet();
            val moteData = aktivitet.getMoteData();

            val publishedMoteReferat = "published";
            aktivitetService.oppdaterReferat(aktivitet,
                    aktivitet.withMoteData(moteData.withReferat("this is fun")),
                    Person.navIdent("Superman"));

            val aktivitetV2 = aktivitetService.hentAktivitet(aktivitet.getId());

            aktivitetService.oppdaterReferat(aktivitetV2,
                    aktivitetV2.withMoteData(moteData.withReferat("test2")),
                    Person.navIdent("Superman"));

            val aktivitetV3 = aktivitetService.hentAktivitet(aktivitet.getId());
            aktivitetService.oppdaterReferat(aktivitetV3,
                    aktivitetV3.withMoteData(moteData.withReferatPublisert(true)),
                    Person.navIdent("Superman"));

            val aktivitetV4 = aktivitetService.hentAktivitet(aktivitet.getId());
            aktivitetService.oppdaterReferat(aktivitetV4,
                    aktivitetV4.withMoteData(moteData.withReferatPublisert(true)
                            .withReferat(publishedMoteReferat)),
                    Person.navIdent("Superman"));


            val hentVersjoner = new HentAktivitetVersjonerRequest();
            hentVersjoner.setAktivitetId(aktivitet.getId().toString());
            val versjoner = aktivitetsplanWS.hentAktivitetVersjoner(hentVersjoner).getAktivitetversjoner();
            assertThat(versjoner, hasSize(3));
            assertThat(versjoner.get(0).getMote().getReferat(), equalTo(publishedMoteReferat));
        }
    }


    @Nested
    public class endreAktivtet {
        @Test
        public void skal_endre_en_aktivitet() throws Exception {
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
            endreReq.setAktivitet(AktivitetWSMapper.mapTilAktivitet(Person.fnr(""), aktivitet));
            val resp = aktivitetsplanWS.endreAktivitet(endreReq);

            assertThat(resp.getAktivitet().getBeskrivelse(), equalTo(nyBeskrivelse));
            assertThat(resp.getAktivitet().getLenke(), equalTo(nyLenke));
        }

        @Test
        public void skal_ikke_endre_en_avtalt_aktivitet() throws Exception {
            opprett_avtalt_aktivitet();
            val aktivitet = aktiviter().get(0);

            val endreReq = new EndreAktivitetRequest();
            val endreAktivitet = AktivitetWSMapper.mapTilAktivitet(Person.fnr(""), aktivitet);
            endreAktivitet.setTom(xmlCalendar(new Date(0)));
            endreAktivitet.setBeskrivelse("bleeeeeee123");
            endreReq.setAktivitet(endreAktivitet);

            Assertions.assertThrows(UgyldigRequest.class, () -> aktivitetsplanWS.endreAktivitet(endreReq));
        }
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
                .egenAktivitetData(EgenAktivitetData.builder().build())
                .build();

        aktivitetService.opprettAktivitet(KJENT_AKTOR_ID, aktivitet, null);
    }

    private AktivitetData opprett_aktivitet_med_type_og_status(AktivitetTypeData aktivitetsType, AktivitetStatus aktivitetsStatus) {
        val aktivitet = nyAktivitet()
                .aktivitetType(aktivitetsType)
                .status(aktivitetsStatus)
                .build();

        long aktivitetId = aktivitetService.opprettAktivitet(KJENT_AKTOR_ID, aktivitet, null);
        return aktivitetService.hentAktivitet(aktivitetId);
    }

    private AktivitetData opprett_mote_aktivitet() {
        val aktivitet = nyAktivitet()
                .aktivitetType(AktivitetTypeData.MOTE)
                .moteData(nyMote())
                .build();

        long aktivitetId = aktivitetService.opprettAktivitet(KJENT_AKTOR_ID, aktivitet, null);
        return aktivitetService.hentAktivitet(aktivitetId);
    }

    private AktivitetData opprett_stilling_aktivitet() {
        val aktivitet = nyAktivitet()
                .aktivitetType(JOBBSOEKING)
                .stillingsSoekAktivitetData(nyttStillingssøk())
                .build();

        long aktivitetId = aktivitetService.opprettAktivitet(KJENT_AKTOR_ID, aktivitet, null);
        return aktivitetService.hentAktivitet(aktivitetId);
    }

    private void opprett_avtalt_aktivitet() {
        val aktivitet = nyAktivitet()
                .aktivitetType(JOBBSOEKING)
                .avtalt(true)
                .stillingsSoekAktivitetData(StillingsoekAktivitetData.builder().build())
                .build();

        aktivitetService.opprettAktivitet(KJENT_AKTOR_ID, aktivitet, null);
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
        hentAktiviteterRequest.setPersonident(KJENT_IDENT.get());
        return hentAktiviteterRequest;
    }

    private Aktivitet nyAktivitetWS() {
        Aktivitet aktivitet = new Aktivitet();
        aktivitet.setPersonIdent(KJENT_IDENT.get());
        aktivitet.setStatus(Status.values()[0]);
        aktivitet.setType(AktivitetType.EGENAKTIVITET);
        return aktivitet;
    }

}