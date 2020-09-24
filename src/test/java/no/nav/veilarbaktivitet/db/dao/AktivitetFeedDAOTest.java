package no.nav.veilarbaktivitet.db.dao;

import lombok.val;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.AktivitetFeedData;
import no.nav.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;

public class AktivitetFeedDAOTest {

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database);
    private AktivitetFeedDAO aktivitetFeedDAO = new AktivitetFeedDAO(database);

    @Before
    public void cleanUp(){
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }

    @Test
    public void hent_aktiviteter_for_feed() {
        val fra = DateUtils.dateFromISO8601("2010-12-03T10:15:30+02:00");
        val endret1 = DateUtils.dateFromISO8601("2010-12-03T10:15:30+02:00");
        val endret2 = DateUtils.dateFromISO8601("2010-12-04T10:15:30+02:00");

        val aktivitet1 = AktivitetDataTestBuilder.nyAktivitet().aktivitetType(AktivitetTypeData.JOBBSOEKING);
        val aktivitet2 = AktivitetDataTestBuilder.nyAktivitet().aktivitetType(AktivitetTypeData.EGENAKTIVITET);
        aktivitetDAO.insertAktivitet(aktivitet1.build(), endret1);
        aktivitetDAO.insertAktivitet(aktivitet2.build(), endret2);

        val hentetAktiviteter = hentAktiviteterEtterTidspunkt(fra);
        Assertions.assertThat(hentetAktiviteter).hasSize(2);
    }

    @Test
    public void hent_aktiviteter_for_feed_skal_hente_bare_en() {
        val fra = DateUtils.dateFromISO8601("2010-12-03T10:15:30.1+02:00");
        val endret1 = DateUtils.dateFromISO8601("2010-12-03T10:15:30+02:00");
        val endret2 = DateUtils.dateFromISO8601("2010-12-03T10:15:30.2+02:00");

        val aktivitet1 = AktivitetDataTestBuilder.nyAktivitet().aktivitetType(AktivitetTypeData.JOBBSOEKING);
        val aktivitet2 = AktivitetDataTestBuilder.nyAktivitet().aktivitetType(AktivitetTypeData.EGENAKTIVITET);
        aktivitetDAO.insertAktivitet(aktivitet1.build(), endret1);
        aktivitetDAO.insertAktivitet(aktivitet2.build(), endret2);

        val hentetAktiviteter = hentAktiviteterEtterTidspunkt(fra);
        Assertions.assertThat(hentetAktiviteter).hasSize(1);
        Assertions.assertThat(hentetAktiviteter.get(0).getEndretDato()).isEqualTo(endret2);
    }

    @Test
    public void hent_aktiviteter_for_feed_skal_returnere_tom_liste() {
        val fra = DateUtils.dateFromISO8601("2010-12-05T11:15:30+02:00");
        val endret1 = DateUtils.dateFromISO8601("2010-12-03T10:15:30+02:00");
        val endret2 = DateUtils.dateFromISO8601("2010-12-04T10:15:30+02:00");

        val aktivitet1 = AktivitetDataTestBuilder.nyAktivitet().aktivitetType(AktivitetTypeData.JOBBSOEKING);
        val aktivitet2 = AktivitetDataTestBuilder.nyAktivitet().aktivitetType(AktivitetTypeData.EGENAKTIVITET);
        aktivitetDAO.insertAktivitet(aktivitet1.build(), endret1);
        aktivitetDAO.insertAktivitet(aktivitet2.build(), endret2);

        val hentetAktiviteter = hentAktiviteterEtterTidspunkt(fra);
        Assertions.assertThat(hentetAktiviteter).isEmpty();
    }

    private List<AktivitetFeedData> hentAktiviteterEtterTidspunkt(ZonedDateTime fra) {
        return aktivitetFeedDAO.hentAktiviteterEtterTidspunkt(fra, 10);
    }

}
