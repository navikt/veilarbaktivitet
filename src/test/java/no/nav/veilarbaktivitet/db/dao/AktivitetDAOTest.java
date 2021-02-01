package no.nav.veilarbaktivitet.db.dao;

import lombok.val;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class AktivitetDAOTest {

    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database);


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
        val aktivitetBuilder = AktivitetDataTestBuilder
                .nyEgenaktivitet()
                .toBuilder();

        Arrays.asList(AktivitetTransaksjonsType.values()).forEach(t -> {
                    aktivitetBuilder.transaksjonsType(t);
                    try {
                        insertAktivitet(aktivitetBuilder.build());
                    } catch (Exception e) {
                        fail("TransaksjonsTypen er ikke lagt inn i databasen");
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

    private AktivitetData gitt_at_det_finnes_en_stillings_aktivitet() {
        val aktivitet = AktivitetDataTestBuilder.nyttStillingssøk();

        return insertAktivitet(AktivitetDataTestBuilder.nyttStillingssøk());
    }

    private AktivitetData gitt_at_det_finnes_en_egen_aktivitet() {
        val aktivitet = AktivitetDataTestBuilder.nyEgenaktivitet();

        return insertAktivitet(aktivitet);
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

    int hentAntallSlettedeAktiviteter() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) as ANTALL_SLETTET FROM SLETTEDE_AKTIVITETER", Integer.class);
    }
}
