package no.nav.veilarbaktivitet.db.dao;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.fail;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {AktivitetDAOTest.Config.class })
@Transactional
public class AktivitetDAOTest {

    @Configuration
    @Import({Database.class, AktivitetDAO.class})
    static class Config {
        @Bean
        public DataSource dataSource(JdbcTemplate jdbcTemplate) {
            return jdbcTemplate.getDataSource();
        }

        @Bean
        public JdbcTemplate jdbcTemplate() {
            return LocalH2Database.getDb();
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager);
        }


    }

    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AktivitetDAO aktivitetDAO;
    @Autowired
    private TransactionTemplate transactionTemplate;


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
                        insertAktivitet(aktivitetData);
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

    @Test
    @Ignore("Versjonering er ikke trådsikker, og derfor sårbar for race-conditions!")
    public void versjonering_skal_vaere_traadsikker() throws InterruptedException {
        // Opprett initiell versjon
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        final AktivitetData aktivitet = transactionTemplate.execute(transactionStatus -> gitt_at_det_finnes_en_egen_aktivitet());

        int antallOppdateringer= 10;
        ExecutorService bakgrunnService = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(antallOppdateringer);
        for (int i = 0; i < antallOppdateringer; i++) {
            bakgrunnService.submit(() -> {
                transactionTemplate.executeWithoutResult( action -> {
                    aktivitetDAO.insertAktivitet(aktivitet.withBeskrivelse("nyBeskrivelse "));
                });
                latch.countDown();
            });
        }
        latch.await();
        List<AktivitetData> aktivitetData = aktivitetDAO.hentAktivitetVersjoner(aktivitet.getId());
        assertThat(aktivitetData, hasSize(antallOppdateringer + 1));
        AktivitetData nyesteAktivitet = aktivitetDAO.hentAktivitet(aktivitet.getId());
        assertThat(nyesteAktivitet, notNullValue());
    }

    private AktivitetData gitt_at_det_finnes_en_stillings_aktivitet() {
        val aktivitet = AktivitetDataTestBuilder.nyttStillingssøk();

        return insertAktivitet(AktivitetDataTestBuilder.nyttStillingssøk());
    }

    private AktivitetData gitt_at_det_finnes_en_egen_aktivitet() {
        val aktivitet = AktivitetDataTestBuilder.nyEgenaktivitet();

        return addAktivitet(aktivitet);
    }

    private AktivitetData gitt_at_det_finnes_en_sokeavtale() {
        val aktivitet = AktivitetDataTestBuilder.nySokeAvtaleAktivitet();

        return insertAktivitet(aktivitet);
    }

    private AktivitetData gitt_at_det_finnes_en_ijobb() {
        return insertAktivitet(AktivitetDataTestBuilder.nyIJobbAktivitet());
    }

    private AktivitetData gitt_at_det_finnes_en_behandling() {
        return insertAktivitet(AktivitetDataTestBuilder.nyBehandlingAktivitet());
    }

    private AktivitetData gitt_at_det_finnes_et_mote() {
        return insertAktivitet(AktivitetDataTestBuilder.nyMoteAktivitet());
    }

    private AktivitetData gitt_at_det_finnes_et_samtalereferat() {
        return insertAktivitet(AktivitetDataTestBuilder.nytSamtaleReferat());
    }

    private AktivitetData gitt_at_det_finnes_en_aktivitet_med_flere_versjoner(int antallVersjoner) {
        AktivitetData et_mote = gitt_at_det_finnes_et_mote();

        AktivitetData versjonX = null;
        for (int i = 1; i < antallVersjoner; i++) {
            versjonX = aktivitetDAO.hentAktivitet(et_mote.getId()).withBeskrivelse("Beskrivelse " + i);
            aktivitetDAO.insertAktivitet(versjonX);
        }
        return versjonX;
    }

    private AktivitetData insertAktivitet(AktivitetData aktivitet) {
        val id = Optional.ofNullable(aktivitet.getId()).orElseGet(aktivitetDAO::getNextUniqueAktivitetId);
        val aktivitetMedId = aktivitet.toBuilder()
                .id(id)
                .aktorId(AKTOR_ID.get())
                .build();

        val endret = new Date();
        aktivitetDAO.insertAktivitet(aktivitetMedId, endret);
        return aktivitetDAO.hentAktivitet(id);
    }

    private AktivitetData addAktivitet(AktivitetData aktivitet) {
        val id = Optional.ofNullable(aktivitet.getId()).orElseGet(aktivitetDAO::getNextUniqueAktivitetId);
        val aktivitetMedId = aktivitet.toBuilder()
                .id(id)
                .aktorId(AKTOR_ID.get())
                .build();

        val endret = new Date();
        aktivitetDAO.insertAktivitet(aktivitetMedId, endret);
        return aktivitetDAO.hentAktivitet(id);
    }
}
