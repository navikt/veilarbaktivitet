package no.nav.veilarbaktivitet.veilarbportefolje;

import lombok.val;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

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
    public void hentOppTil5000MeldingerSomIkkeErSendt_AktivitetErSendOgIkkeSendt_returnererKunUsendte() {
        val usendt = opprettEgenaktivitet();
        val sendt = opprettEgenaktivitet();

        val usendteAktiviteter_pre = kafkaAktivitetDAO.hentOppTil5000MeldingerSomIkkeErSendt();
        Assert.assertTrue(containsVersion(usendteAktiviteter_pre, usendt.getVersjon()));
        Assert.assertTrue(containsVersion(usendteAktiviteter_pre, sendt.getVersjon()));

        kafkaAktivitetDAO.updateSendtPaKafka(sendt.getVersjon(), 1L);
        val usendteAktiviteter_post = kafkaAktivitetDAO.hentOppTil5000MeldingerSomIkkeErSendt();
        Assert.assertTrue(containsVersion(usendteAktiviteter_post, usendt.getVersjon()));
        Assert.assertFalse(containsVersion(usendteAktiviteter_post, sendt.getVersjon()));
    }

    @Test
    public void hentOppTil5000MeldingerSomIkkeErSendt_AktivitetErIkkeSendt_returnererEnAktivitet() {
        val aktivitet = opprettEgenaktivitet();
        val usendteAktiviteter = kafkaAktivitetDAO.hentOppTil5000MeldingerSomIkkeErSendt();
        Assert.assertTrue(containsVersion(usendteAktiviteter, aktivitet.getVersjon()));
    }

    @Test
    public void hentOppTil5000MeldingerSomIkkeErSendt_kunSendteAktiviteter_returnererIngen() {
        val aktivitet = opprettEgenaktivitet();
        val usendteAktiviteter = kafkaAktivitetDAO.hentOppTil5000MeldingerSomIkkeErSendt();
        Assert.assertTrue(containsVersion(usendteAktiviteter, aktivitet.getVersjon()));

        kafkaAktivitetDAO.updateSendtPaKafka(aktivitet.getVersjon(), 1L);
        val usendteAktiviteter_tom = kafkaAktivitetDAO.hentOppTil5000MeldingerSomIkkeErSendt();
        Assert.assertFalse(containsVersion(usendteAktiviteter_tom, aktivitet.getVersjon()));

    }

    private boolean containsVersion(List<KafkaAktivitetMeldingV4> list, Long version){
        return list.stream()
                .filter(e -> e.getVersion().equals(version))
                .count() == 1;
    }

    private AktivitetData opprettEgenaktivitet() {
        val aktivitet = AktivitetDataTestBuilder.nyEgenaktivitet();

        return insertAktivitet(aktivitet);
    }

    private AktivitetData insertAktivitet(AktivitetData aktivitet) {
        val nyAktivitet = aktivitet.toBuilder()
                .aktorId(AKTOR_ID.get())
                .build();

        return aktivitetDAO.opprettNyAktivitet(nyAktivitet);
    }

}
