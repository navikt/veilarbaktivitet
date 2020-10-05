package no.nav.veilarbaktivitet.db.dao;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.domain.SmsAktivitetData;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.veilarbaktivitet.db.Database.hentDatoMedTidssone;

@Component
@Slf4j
public class MoteSmsDAO {

    private final Database database;

    public MoteSmsDAO(Database database) {
        this.database = database;
    }

    public List<SmsAktivitetData> hentIkkeAvbrutteMoterMellom(ZonedDateTime fra, ZonedDateTime til) {

        log.info("henter moter mellom " + fra + " og " + til);

        //language=sql
        return database.query(
                "select " +
                        " AKTOR_ID " +
                        ", AKTIVITET.AKTIVITET_ID as ID " +
                        ", AKTIVITET.VERSJON as AKTIVITET_VERSJON" +
                        ", FRA_DATO " +
                        ", MOTETID " +
                        ", MOTE.KANAL as AKTIVITET_KANAL" +
                        ", GJELDENDE_MOTE_SMS.KANAL as SMS_KANAL"+
                        " from AKTIVITET" +
                        " left join MOTE on AKTIVITET.AKTIVITET_ID = MOTE.AKTIVITET_ID and AKTIVITET.VERSJON = MOTE.VERSJON"+
                        " left join GJELDENDE_MOTE_SMS on AKTIVITET.AKTIVITET_ID = GJELDENDE_MOTE_SMS.AKTIVITET_ID" +
                        " where AKTIVITET_TYPE_KODE  = 'MOTE'" +
                        " and GJELDENDE = 1" +
                        " and LIVSLOPSTATUS_KODE != 'AVBRUTT'" +
                        " and FRA_DATO between ? and ?" +
                        " and HISTORISK_DATO is null " +
                        " order by FRA_DATO"
                ,
                this::mapper,
                fra,
                til
                );
    }


    private SmsAktivitetData mapper(ResultSet rs) throws SQLException {
        return SmsAktivitetData
                .builder()
                .aktorId(rs.getString("AKTOR_ID"))
                .aktivitetId(rs.getLong("ID"))
                .aktivtetVersion(rs.getLong("AKTIVITET_VERSJON"))
                .moteTidAktivitet(hentDatoMedTidssone(rs, "FRA_DATO"))
                .smsSendtMoteTid(hentDatoMedTidssone(rs, "MOTETID"))
                .aktivitetKanal(rs.getString("AKTIVITET_KANAL"))
                .smsKanal(rs.getString("SMS_KANAL"))
                .build();
    }

    public void insertSmsSendt(SmsAktivitetData smsAktivitetData, String varselId) {
        Timestamp moteTid = Timestamp.valueOf(smsAktivitetData.getMoteTidAktivitet().toLocalDateTime());
        Timestamp now = Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime());
        Long aktiviteteId = smsAktivitetData.getAktivitetId();
        Long aktivtetVersion = smsAktivitetData.getAktivtetVersion();
        String kanal = smsAktivitetData.getAktivitetKanal();

        //language=sql
        int antall = database.update(
                "update GJELDENDE_MOTE_SMS set MOTETID = ?, KANAL = ? where AKTIVITET_ID = ?"
                , moteTid
                , kanal
                , aktiviteteId
        );

        if (antall == 0) {
            //language=sql
            database.update(
                    "insert into GJELDENDE_MOTE_SMS (AKTIVITET_ID, MOTETID, KANAL)" +
                            " values (?, ?, ?)"
                    , aktiviteteId
                    , moteTid
                    , kanal
            );
        }

        //language=sql
        database.update(
                "insert into MOTE_SMS_HISTORIKK" +
                        " (AKTIVITET_ID, VERSJON, MOTETID, VARSEL_ID, KANAL, SENDT) VALUES" +
                        " (?,?,?,?,?,?)"
                , aktiviteteId
                , aktivtetVersion
                , moteTid
                , varselId
                , kanal
                , now
        );
    }

    public long antallAktivteterSendtSmsPaa() {
        //language=sql
        return database.queryForObject("select count(*) from GJELDENDE_MOTE_SMS", Long.class);
    }

    public long antallSmsSendt() {
        //language=sql
        return database.queryForObject("select count(*) from MOTE_SMS_HISTORIKK", Long.class);
    }
}
