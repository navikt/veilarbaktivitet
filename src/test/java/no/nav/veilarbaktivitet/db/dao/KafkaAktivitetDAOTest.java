package no.nav.veilarbaktivitet.db.dao;

import lombok.val;
import no.nav.veilarbaktivitet.aktiviterTilKafka.KafkaAktivitetDAO;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

public class KafkaAktivitetDAOTest {

    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database);
    private final KafkaAktivitetDAO kafkaAktivitetDAO = new KafkaAktivitetDAO(database);


    @Before
    public void cleanUp(){
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }


    @Test
    public void opprette_og_hente_melding_som_ikke_er_sendt() {
        val aktivitet = gitt_at_det_finnes_en_egen_aktivitet();

        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);
        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
        val usendteAktiviteter = kafkaAktivitetDAO.hentOppTil5000MeldingerSomIkkeErSendt();
        assertThat(usendteAktiviteter, hasSize(1));

    }

    @Test
    public void marker_melding_som_sendt() {
        val aktivitet = gitt_at_det_finnes_en_egen_aktivitet();

        val aktiviteter = aktivitetDAO.hentAktiviteterForAktorId(AKTOR_ID);
        assertThat(aktiviteter, hasSize(1));
        assertThat(aktivitet, equalTo(aktiviteter.get(0)));
        val usendteAktiviteter = kafkaAktivitetDAO.hentOppTil5000MeldingerSomIkkeErSendt();
        assertThat(usendteAktiviteter, hasSize(1));

        kafkaAktivitetDAO.updateVeilarbOffset(usendteAktiviteter.get(0), 1L);

        val usendteAktiviteter_tom = kafkaAktivitetDAO.hentOppTil5000MeldingerSomIkkeErSendt();
        assertThat(usendteAktiviteter_tom, hasSize(0));

    }

    private AktivitetData gitt_at_det_finnes_en_egen_aktivitet() {
        val aktivitet = AktivitetDataTestBuilder.nyEgenaktivitet();

        return insertAktivitet(aktivitet);
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

}
