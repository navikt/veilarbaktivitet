package no.nav.veilarbaktivitet.db.dao;

import no.nav.veilarbaktivitet.AktivitetDataTestBuilder;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.DbTestUtils;
import no.nav.veilarbaktivitet.domain.*;
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
        AktivitetData aktivitetData = insertMote(10, new Date());

        moteSmsDAO.insertSmsSendt(aktivitetData.getId(), aktivitetData.getVersjon(), new Date(), "kake");

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

        AktivitetData mote1 = insertMote(10, date_0);
        AktivitetData mote2 = insertMote(12, date_2);
        AktivitetData mote1_2 = insertMote(10, date_0);

        moteSmsDAO.insertSmsSendt(mote1.getId(), mote1.getVersjon(), date_0, "kake");
        moteSmsDAO.insertSmsSendt(mote2.getId(), mote2.getVersjon(), date_2, "kake1");
        moteSmsDAO.insertSmsSendt(mote1_2.getId(), mote1_2.getVersjon(), date, "kake2");

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
    public void skalHenteMoterMellom() {
        insertMote(2, betwheen2);
        insertMote(4, betwheen);

        List<SmsAktivitetData> smsAktivitetData = moteSmsDAO.hentIkkeAvbrutteMoterMellom(earlyCuttoff, lateCuttof);

        assertThat(smsAktivitetData.size()).isEqualTo(2);

        SmsAktivitetData aktivitetData = smsAktivitetData.get(0);

        assertThat(aktivitetData.getAktivitetId()).isEqualTo(4);

        assertThat(smsAktivitetData.get(1).getAktorId()).isEqualTo("1234");

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
        insertAktivitet(1l, betwheen, AktivitetTypeData.MOTE,AktivitetStatus.AVBRUTT);

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

    private Date createDate(int hour) {
        return Date.from(LocalDateTime.of(2016,1, 1, hour,1).toInstant(ZoneOffset.UTC));
    }


    private AktivitetData insertMote(long id, Date fraDato) {
        return insertAktivitet(id, fraDato, AktivitetTypeData.MOTE, AktivitetStatus.GJENNOMFORES);
    }

    private AktivitetData insertAktivitet(long id, Date fraDato, AktivitetTypeData type, AktivitetStatus aktivitetStatus) {
        AktivitetData aktivitet = AktivitetDataTestBuilder
                .nyAktivitet()
                .id(id)
                .aktivitetType(type)
                .status(aktivitetStatus)
                .fraDato(fraDato)
                .aktorId(AKTOR_ID.get())
                .build();

        aktivitetDAO.insertAktivitet(aktivitet);

        return aktivitetDAO.hentAktivitet(aktivitet.getId());
    }

    private long selectCountFrom(String tabelname, JdbcTemplate template) {
        return template.queryForObject("select count(*) from " + tabelname, Long.class);
    }
}
