package no.nav.veilarbaktivitet.db.dao;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.base.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.base.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
@Transactional
public class AktivitetDAOTest {

    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AktivitetDAO aktivitetDAO;
    @Autowired
    private TransactionTemplate transactionTemplate;


    @After
    @Before
    public void cleanUp(){
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }

    @Test
    public void opprette_og_hente_egenaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_egen_aktivitet();

        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);
        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_stillingaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);
        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_sokeavtaleaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_sokeavtale();

        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);
        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_ijobbaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_ijobb();
        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);

        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_behandlingaktivitet() {
        val aktivitet = gitt_at_det_finnes_en_behandling();
        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);

        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_mote() {
        val aktivitet = gitt_at_det_finnes_et_mote();
        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);

        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_samtalereferat() {
        val aktivitet = gitt_at_det_finnes_et_samtalereferat();
        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);

        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
    }

    @Test
    public void opprette_og_hente_stillingFraNAV() {
        var aktivitet = gitt_at_det_finnes_en_stillingFraNav();
        var aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);

        assertEquals(1, aktiviteter.size());
        assertEquals(aktivitet, aktiviteter.get(0));
    }

    @Test
    public void hent_aktivitet() {
        val aktivitet = gitt_at_det_finnes_en_stillings_aktivitet();

        val hentetAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        assertThat(aktivitet, equalTo(hentetAktivitet));
    }

    @Test
    public void transaksjonsTypene_er_rett_satt_opp() {
        Arrays.asList(AktivitetTransaksjonsType.values()).forEach(t -> {
                    val aktivitetData = AktivitetDataTestBuilder
                            .nyEgenaktivitet()
                            .withTransaksjonsType(t)
                            ;
                    try {
                        addAktivitet(aktivitetData);
                    } catch (Exception e) {
                        fail("TransaksjonsTypen " + t + " er ikke lagt inn i databasen");
                    }
                }
        );
    }

    @Test
    public void skal_legge_til_lest_av_bruker_forste_gang() {
        val aktivitet = gitt_at_det_finnes_en_egen_aktivitet();
        aktivitetDAO.insertLestAvBrukerTidspunkt(aktivitet.getId());

        val hentetAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        assertThat(hentetAktivitet.getLestAvBrukerForsteGang(), is(notNullValue()));
    }

    @Test
    public void skal_hente_alle_aktivitets_versjoner() {
        val aktivitet = gitt_at_det_finnes_en_aktivitet_med_flere_versjoner(3);
        List<AktivitetData> aktivitetData = aktivitetDAO.hentAktivitetVersjoner(aktivitet.getId());
        assertThat(aktivitetData, hasSize(3));
    }

    @Test(expected = IllegalStateException.class)
    public void kan_ikke_oppdatere_gammel_versjon() {
        AktivitetData originalAktivitetVersjon = gitt_at_det_finnes_en_egen_aktivitet();
        AktivitetData nyAktivitetVersjon = naar_jeg_oppdaterer_en_aktivitet(originalAktivitetVersjon);
        assertThat(nyAktivitetVersjon.getVersjon(), is(greaterThan(originalAktivitetVersjon.getVersjon())));
        // Kan jeg ikke oppdatere den originale aktivitetVersjonen lenger
        naar_jeg_oppdaterer_en_aktivitet(originalAktivitetVersjon);
    }


    @Test
    public void versjonering_skal_vaere_traadsikker() throws InterruptedException {
        // Opprett initiell versjon
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        final AktivitetData aktivitet  = transactionTemplate.execute(transactionStatus -> gitt_at_det_finnes_en_egen_aktivitet());
        int antallOppdateringer= 10;
        ExecutorService bakgrunnService = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(antallOppdateringer);
        for (int i = 0; i < antallOppdateringer; i++) {
            bakgrunnService.submit(() -> {
                transactionTemplate.executeWithoutResult( action -> {
                    try {
                        aktivitetDAO.oppdaterAktivitet(aktivitet.withBeskrivelse("nyBeskrivelse "));
                    } catch (Exception e) {
                        log.warn("Feil i tråd.", e);
                    } finally {
                        latch.countDown();
                    }
                });
            });
        }
        latch.await();
        List<AktivitetData> aktivitetData = aktivitetDAO.hentAktivitetVersjoner(aktivitet.getId());
        // Kun originalversjonen pluss første oppdatering. Resten feiler
        assertThat(aktivitetData, hasSize(2));
        // Denne vil feile hvis det er mer enn én gjeldende
        AktivitetData nyesteAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        assertThat(nyesteAktivitet, notNullValue());
    }

    private AktivitetData gitt_at_det_finnes_en_stillings_aktivitet() {
        val aktivitet = AktivitetDataTestBuilder.nyttStillingssok();

        return addAktivitet(aktivitet);
    }

    private AktivitetData gitt_at_det_finnes_en_egen_aktivitet() {
        val aktivitet = AktivitetDataTestBuilder.nyEgenaktivitet();

        return addAktivitet(aktivitet);
    }

    private AktivitetData gitt_at_det_finnes_en_sokeavtale() {
        val aktivitet = AktivitetDataTestBuilder.nySokeAvtaleAktivitet();

        return addAktivitet(aktivitet);
    }

    private AktivitetData gitt_at_det_finnes_en_ijobb() {
        return addAktivitet(AktivitetDataTestBuilder.nyIJobbAktivitet());
    }

    private AktivitetData gitt_at_det_finnes_en_behandling() {
        return addAktivitet(AktivitetDataTestBuilder.nyBehandlingAktivitet());
    }

    private AktivitetData gitt_at_det_finnes_et_mote() {
        return addAktivitet(AktivitetDataTestBuilder.nyMoteAktivitet());
    }

    private AktivitetData gitt_at_det_finnes_et_samtalereferat() {
        return addAktivitet(AktivitetDataTestBuilder.nytSamtaleReferat());
    }

    private AktivitetData gitt_at_det_finnes_en_stillingFraNav() {
        return addAktivitet(AktivitetDataTestBuilder.nyStillingFraNavMedCVKanDeles());
    }

    private AktivitetData naar_jeg_oppdaterer_en_aktivitet(AktivitetData aktivitetData) {
        return aktivitetDAO.oppdaterAktivitet(aktivitetData);
    }

    private AktivitetData gitt_at_det_finnes_en_aktivitet_med_flere_versjoner(int antallVersjoner) {
        AktivitetData et_mote = gitt_at_det_finnes_et_mote();

        AktivitetData versjonX = null;
        for (int i = 1; i < antallVersjoner; i++) {
            versjonX = aktivitetDAO.hentAktivitet(et_mote.getId()).withBeskrivelse("Beskrivelse " + i);
            aktivitetDAO.oppdaterAktivitet(versjonX);
        }
        return versjonX;
    }

    private AktivitetData addAktivitet(AktivitetData aktivitet) {
        val aktivitetUtenId = aktivitet.toBuilder()
                .aktorId(AKTOR_ID.get())
                .build();

        return aktivitetDAO.opprettNyAktivitet(aktivitetUtenId);
    }
}
