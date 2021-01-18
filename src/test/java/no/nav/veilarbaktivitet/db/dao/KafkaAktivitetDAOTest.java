package no.nav.veilarbaktivitet.db.dao;

import lombok.val;
import no.nav.veilarbaktivitet.aktiviterTilKafka.KafkaAktivitetDAO;
import no.nav.veilarbaktivitet.aktiviterTilKafka.KafkaAktivitetMeldingV4;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;
import java.util.List;
import java.util.Optional;

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
    public void hentOppTil5000MeldingerSomIkkeErSendt_AktivitetErSendOgIkkeSendt_returnererKunDeUsendte() {
        val usendt_1 = opprettEgenaktivitet();
        val usendt_2 = opprettEgenaktivitet();
        val usendt_3 = opprettEgenaktivitet();
        val sendt_1 = opprettEgenaktivitet();

        val usendteAktiviteter_pre = kafkaAktivitetDAO.hentOppTil5000MeldingerSomIkkeErSendt();
        Assert.assertTrue(containsVersion(usendteAktiviteter_pre, usendt_1));
        Assert.assertTrue(containsVersion(usendteAktiviteter_pre, usendt_2));
        Assert.assertTrue(containsVersion(usendteAktiviteter_pre, usendt_3));
        Assert.assertTrue(containsVersion(usendteAktiviteter_pre, sendt_1));

        kafkaAktivitetDAO.updateSendtPaKafka(sendt_1.getVersjon(), 1L);
        val usendteAktiviteter_post = kafkaAktivitetDAO.hentOppTil5000MeldingerSomIkkeErSendt();
        Assert.assertTrue(containsVersion(usendteAktiviteter_post, usendt_1));
        Assert.assertTrue(containsVersion(usendteAktiviteter_post, usendt_2));
        Assert.assertTrue(containsVersion(usendteAktiviteter_post, usendt_3));
        Assert.assertFalse(containsVersion(usendteAktiviteter_post, sendt_1));
    }

    @Test
    public void hentOppTil5000MeldingerSomIkkeErSendt_AktivitetErIkkeSendt_returnererEnAktivitet() {
        val aktivitet = opprettEgenaktivitet();
        val usendteAktiviteter = kafkaAktivitetDAO.hentOppTil5000MeldingerSomIkkeErSendt();
        Assert.assertTrue(containsVersion(usendteAktiviteter, aktivitet));
    }

    @Test
    public void hentOppTil5000MeldingerSomIkkeErSendt_kunSendteAktiviteter_returnererIngen() {
        val aktivitet = opprettEgenaktivitet();
        val usendteAktiviteter = kafkaAktivitetDAO.hentOppTil5000MeldingerSomIkkeErSendt();
        Assert.assertTrue(containsVersion(usendteAktiviteter, aktivitet));

        kafkaAktivitetDAO.updateSendtPaKafka(aktivitet.getVersjon(), 1L);
        val usendteAktiviteter_tom = kafkaAktivitetDAO.hentOppTil5000MeldingerSomIkkeErSendt();
        Assert.assertFalse(containsVersion(usendteAktiviteter_tom, aktivitet));

    }

    private boolean containsVersion(List<KafkaAktivitetMeldingV4> list, AktivitetData aktivitet){
        return list.stream()
                .filter(e -> e.getVersion().equals(aktivitet.getVersjon())).count() == 1;
    }

    private AktivitetData opprettEgenaktivitet() {
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
