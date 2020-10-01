package no.nav.veilarbaktivitet.db.dao;

import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MoteSMSDAOTest {
    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database);
    private final MoteSmsDAO moteSmsDAO = new MoteSmsDAO(database);


    private final ZonedDateTime bofore = createDate(1);
    private final ZonedDateTime earlyCuttoff = createDate(2);
    private final ZonedDateTime betwheen = createDate(3);
    private final ZonedDateTime betwheen2 = createDate(4);
    private final ZonedDateTime lateCuttof = createDate(5);
    private final ZonedDateTime after = createDate(6);

    @Before
    public void cleanUp(){
        System.setProperty("AKTIVITETSPLAN_URL", "aktivitesplan_url");
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }

    @Test
    public void skalHenteMoterMellom() {
        insertMote(2, betwheen2);
        AktivitetData aktivitetData2 = insertMote(4, betwheen);

        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentIkkeAvbrutteMoterMellom(earlyCuttoff, lateCuttof);

        assertThat(smsAktivitetData.size()).isEqualTo(2);

        SmsAktivitetData aktivitetData = smsAktivitetData.get(0);

        assertThat(aktivitetData.getAktivitetId()).isEqualTo(4);

        assertThat(smsAktivitetData.get(1).getAktorId()).isEqualTo(aktivitetData2.getAktorId());

    }

    @Test
    public void skalHenteEnVersonAvMote() {
        insertMote(2, betwheen2);
        insertMote(2, betwheen);

        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentIkkeAvbrutteMoterMellom(earlyCuttoff, lateCuttof);

        assertThat(smsAktivitetData.size()).isEqualTo(1);

    }

    @Test
    public void skalIkkeHenteAvbrutt() {
        insertAbrutMote(1l, betwheen);

        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentIkkeAvbrutteMoterMellom(earlyCuttoff, lateCuttof);

        assertThat(smsAktivitetData.size()).isEqualTo(0);

    }

    @Test
    public void skalIkkeHneteMoterUtenfor() {
        insertMote(2,bofore);
        insertMote(3, after);

        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentIkkeAvbrutteMoterMellom(earlyCuttoff, lateCuttof);

        assertThat(smsAktivitetData.size()).isEqualTo(0);
    }

    private ZonedDateTime createDate(int hour) {
        LocalDateTime localTime = LocalDateTime.of(2016,1, 1, hour,1);
        return ZonedDateTime.of(localTime, ZoneId.systemDefault());
    }


    private AktivitetData insertMote(long id, ZonedDateTime fraDato) {
        AktivitetData build = AktivitetDataTestBuilder
                .nyMoteAktivitet()
                .toBuilder()
                .status(AktivitetStatus.GJENNOMFORES)
                .id(id)
                .fraDato(fraDato)
                .build();

        aktivitetDAO.insertAktivitet(build);
        return aktivitetDAO.hentAktivitet(id);
    }

    private AktivitetData insertAbrutMote(long id, ZonedDateTime fraDato) {
        AktivitetData build = AktivitetDataTestBuilder
                .nyMoteAktivitet()
                .toBuilder()
                .status(AktivitetStatus.AVBRUTT)
                .id(id)
                .fraDato(fraDato)
                .build();

        aktivitetDAO.insertAktivitet(build);
        return aktivitetDAO.hentAktivitet(id);
    }
}
