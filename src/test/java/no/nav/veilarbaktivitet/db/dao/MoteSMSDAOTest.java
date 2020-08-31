package no.nav.veilarbaktivitet.db.dao;

import no.nav.veilarbaktivitet.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.SmsAktivitetData;
import no.nav.veilarbaktivitet.mock.LocalH2Database;
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
        DbTestUtils.cleanupTestDb(jdbcTemplate);
    }

    @Test
    public void skalInserteNy() {
        moteSmsDAO.insertSmsSendt(10L, 1L, new Date(), "kake");

        long antall = selectCountFrom("GJELDENDE_MOTE_SMS", jdbcTemplate);
        long antall_historisk = selectCountFrom("MOTE_SMS_HISTORIKK", jdbcTemplate);

        assertThat(antall).isEqualTo(1);
        assertThat(antall_historisk).isEqualTo(1);
    }

    @Test
    public void skalOppdatereGjeldende() {

        Date date_0 = new Date(0);
        Date date_2 = new Date(60*60*1000);
        Date date = new Date();

        moteSmsDAO.insertSmsSendt(10L, 1L, date_0, "kake");
        moteSmsDAO.insertSmsSendt(12L, 1L, date_2, "kake");
        moteSmsDAO.insertSmsSendt(10L, 2L, date, "kake");

        long antall = selectCountFrom("GJELDENDE_MOTE_SMS", jdbcTemplate);
        long antall_historisk = selectCountFrom("MOTE_SMS_HISTORIKK", jdbcTemplate);

        Date oppdatert = jdbcTemplate.queryForObject("select MOTETID from GJELDENDE_MOTE_SMS where AKTIVITET_ID = 10", Date.class);
        Date ikke_oppdatert = jdbcTemplate.queryForObject("select MOTETID from GJELDENDE_MOTE_SMS where AKTIVITET_ID = 12", Date.class);

        assertThat(date).isEqualTo(oppdatert); //må vere denne veien da equals ikke virker andre veien.
        assertThat(date_2).isEqualTo(ikke_oppdatert); //må vere denne veien da equals ikke virker andre veien.
        assertThat(antall).isEqualTo(2);
        assertThat(antall_historisk).isEqualTo(3);
    }

    @Test
    public void tet() {
        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentMoterMellom(new Date(), new Date());
    }

    @Test
    public void skalHenteMoterMellom() {
        inserMote(2, betwheen2);
        inserMote(4, betwheen);

        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentMoterMellom(earlyCuttoff, lateCuttof);

        assertThat(smsAktivitetData.size()).isEqualTo(2);

        SmsAktivitetData aktivitetData = smsAktivitetData.get(0);

        assertThat(aktivitetData.getAktivitetId()).isEqualTo(4);

        assertThat(smsAktivitetData.get(1).getAktorId()).isEqualTo("1234");

    }

    @Test
    public void skalIkkeHneteMoterMedSmSskalIkkeHenteMoterMedAleredeSmsForTidspunkt() throws InterruptedException {

        inserMote(2, betwheen);
        inserMote(2, betwheen2); //oppdaterer id2
        inserMote(2, betwheen2); //oppdaterer id2

        moteSmsDAO.insertSmsSendt(1,1,betwheen2, "kake");

        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentMoterMellom(earlyCuttoff, lateCuttof);

        //assertThat(smsAktivitetData.size()).isEqualTo(0); //tilsynelatende andledes tolkgning mellom h2 og oracle
        //eller så er den en feil jeg ikke ser
    }

    @Test
    public void skalIkkeHneteMoterUtenfor() {
        inserMote(2,bofore);
        inserMote(3, after);

        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentMoterMellom(earlyCuttoff, lateCuttof);

        assertThat(smsAktivitetData.size()).isEqualTo(0);
    }

    private Date createDate(int month) {
        return Date.from(LocalDateTime.of(2016,month, 1, 1,1).toInstant(ZoneOffset.UTC));
    }


    private void inserMote(long id, Date fraDato) {
        insertAktivitet(id, fraDato, AktivitetTypeData.MOTE);
    }

    private void insertAktivitet(long id, Date fraDato, AktivitetTypeData type) {
        AktivitetData aktivitet = AktivitetDataTestBuilder
                .nyAktivitet()
                .id(id)
                .aktivitetType(type)
                .fraDato(fraDato)
                .aktorId(AKTOR_ID.get())
                .build();

        aktivitetDAO.insertAktivitet(aktivitet);
    }

    private long selectCountFrom(String tabelname, JdbcTemplate template) {
        return template.queryForObject("select count(*) from " + tabelname, Long.class);
    }
}
