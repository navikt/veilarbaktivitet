package no.nav.veilarbaktivitet.db.dao;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.domain.SmsAktivitetData;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class MoteSmsDAO {

    private final Database database;

    public MoteSmsDAO(Database database) {
        this.database = database;
    }


    public List<SmsAktivitetData> hentIkkeAvbrutteMoterMellom(Date fra, Date til) {

        log.info("henter moter mellom " + fra + " og " + til);

        //language=sql
        return database.query(
                "select " +
                        " AKTOR_ID " +
                        ", AKTIVITET.AKTIVITET_ID " +
                        ", AKTIVITET.versjon " +
                        ", FRA_DATO " +
                        ", MOTETID "+
                        " from AKTIVITET" +
                        " left join GJELDENDE_MOTE_SMS on AKTIVITET.AKTIVITET_ID = GJELDENDE_MOTE_SMS.AKTIVITET_ID" +
                        " where AKTIVITET_TYPE_KODE  = 'MOTE'" +
                        " and GJELDENDE = 1" +
                        " and livslopstatus_kode != 'AVBRUTT'" +
                        " and FRA_DATO between ? and ?" +
                        " order by FRA_DATO asc"
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
                .aktivitetId(rs.getLong("AKTIVITET.AKTIVITET_ID"))
                .aktivtetVersion(rs.getLong("AKTIVITET.versjon"))
                .MoteTidAktivitet(rs.getDate("FRA_DATO"))
                .SmsSendtMoteTid(rs.getDate("MOTETID"))
                .build();
    }

    public void insertSmsSendt(long aktiviteteId, long aktivtetVerson, Date motetTid, String varselId) {
        //language=sql
        int antall = database.update(
                "update GJELDENDE_MOTE_SMS" +
                        " set MOTETID = ?" +
                        " where AKTIVITET_ID = ?"
                , motetTid
                , aktiviteteId
        );

        if (antall == 0) {
            //language=sql
            database.update(
                    "insert into GJELDENDE_MOTE_SMS (AKTIVITET_ID, MOTETID)" +
                            " values (?, ?)"
                    , aktiviteteId
                    , motetTid
            );
        }

        //language=sql
        database.update(
                "insert into MOTE_SMS_HISTORIKK" +
                        " (AKTIVITET_ID, VERSJON, MOTETID, VARSEL_ID, SENDT) VALUES" +
                        " (?,?,?,?,?)"
                , aktiviteteId
                , aktivtetVerson
                , motetTid
                , varselId
                , new Date()
        );
    }
}
