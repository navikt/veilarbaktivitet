package no.nav.veilarbaktivitet.motesms;

import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MoteSMSDAOTest {
    private static final Person.AktorId AKTOR_ID = Person.aktorId("1234");

    private final JdbcTemplate jdbcTemplate = LocalH2Database.getDb();
    private final Database database = new Database(jdbcTemplate);
    private final AktivitetDAO aktivitetDAO = new AktivitetDAO(database);
    private final MoteSmsDAO moteSmsDAO = new MoteSmsDAO(database);


    private final Date bofore = createDate(1);
    private final Date earlyCuttoff = createDate(2);
    private final Date betwheen = createDate(3);
    private final Date betwheen2 = createDate(4);
    private final Date lateCuttof = createDate(5);
    private final Date after = createDate(6);

    @Before
    public void cleanUp(){
        System.setProperty("AKTIVITETSPLAN_URL", "aktivitesplan_url");
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }

    @Test
    public void skalHenteMoterMellom() {
        AktivitetData aktivitetData1 = opprettMote(betwheen2);
        AktivitetData aktivitetData2 = opprettMote(betwheen);

        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentIkkeAvbrutteMoterMellom(earlyCuttoff, lateCuttof);

        assertThat(smsAktivitetData.size()).isEqualTo(2);

        SmsAktivitetData aktivitetData = smsAktivitetData.get(0);

        assertThat(aktivitetData.getAktivitetId()).isEqualTo(aktivitetData2.getId());

        assertThat(smsAktivitetData.get(1).getAktorId()).isEqualTo(aktivitetData2.getAktorId());

    }

    @Test
    public void skalHenteEnVersonAvMote() {
        AktivitetData mote = opprettMote(betwheen2);
        oppdaterMote(mote, betwheen);

        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentIkkeAvbrutteMoterMellom(earlyCuttoff, lateCuttof);

        assertThat(smsAktivitetData.size()).isEqualTo(1);

    }

    @Test
    public void skalIkkeHenteAvbrutt() {
        opprettAvbruttMote(betwheen);

        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentIkkeAvbrutteMoterMellom(earlyCuttoff, lateCuttof);

        assertThat(smsAktivitetData.size()).isEqualTo(0);

    }

    @Test
    public void skalIkkeHneteMoterUtenfor() {
        opprettMote(bofore);
        opprettMote(after);

        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentIkkeAvbrutteMoterMellom(earlyCuttoff, lateCuttof);

        assertThat(smsAktivitetData.size()).isEqualTo(0);
    }

    private Date createDate(int hour) {
        return Date.from(LocalDateTime.of(2016,1, 1, hour,1).toInstant(ZoneOffset.UTC));
    }


    private AktivitetData opprettMote(Date fraDato) {
        AktivitetData build = AktivitetDataTestBuilder
                .nyMoteAktivitet()
                .toBuilder()
                .status(AktivitetStatus.GJENNOMFORES)
                .fraDato(fraDato)
                .build();

        return aktivitetDAO.opprettNyAktivitet(build);
    }

    private void oppdaterMote(AktivitetData mote, Date fraDato) {
        AktivitetData build = mote
                .toBuilder()
                .fraDato(fraDato)
                .build();
        aktivitetDAO.oppdaterAktivitet(build);
    }

    private AktivitetData opprettAvbruttMote(Date fraDato) {
        AktivitetData build = AktivitetDataTestBuilder
                .nyMoteAktivitet()
                .toBuilder()
                .status(AktivitetStatus.AVBRUTT)
                .fraDato(fraDato)
                .build();

        return aktivitetDAO.opprettNyAktivitet(build);
    }

    private long selectCountFrom(String tabelname, JdbcTemplate template) {
        return template.queryForObject("select count(*) from " + tabelname, Long.class);
    }
}
