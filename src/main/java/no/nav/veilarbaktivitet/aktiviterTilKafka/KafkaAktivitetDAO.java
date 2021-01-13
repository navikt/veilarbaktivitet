package no.nav.veilarbaktivitet.aktiviterTilKafka;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import lombok.val;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.db.rowmappers.AktivitetDataRowMapper;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static no.nav.veilarbaktivitet.db.dao.AktivitetDAO.SELECT_AKTIVITET;
import static no.nav.veilarbaktivitet.mappers.Helpers.typeMap;

@Repository
@AllArgsConstructor
public class KafkaAktivitetDAO {
    private final Database database;

    @Timed
    public List<KafkaAktivitetMeldingV3> hentOppTil5000MeldingerUtenKafka() {
        // language=sql
        return database.query(""+
                        " select AKTOR_ID, AKTIVITET.AKTIVITET_ID as ID, VERSJON,  FRA_DATO, TIL_DATO, ENDRET_DATO, AKTIVITET_TYPE_KODE, LIVSLOPSTATUS_KODE, AVTALT, HISTORISK_DATO " +
                        " from AKTIVITET " +
                        " where GJELDENDE = 1" +
                        " and not exists(" +
                        " select 1 from AKTIVITET_SENDT_PAA_KAFKA_V3 kafka " +
                        " where kafka.AKTIVITET_ID = AKTIVITET.AKTIVITET_ID " +
                        " and kafka.AKTIVITET_VERSJON = AKTIVITET.VERSJON" +
                        " ) order by VERSJON desc " +
                        " FETCH NEXT 5000 ROWS ONLY",
                KafkaAktivitetDAO::mapKafkaAktivitetMeldingV3
        );
    }

    @Timed
    public List<KafkaAktivitetMeldingV4> hentOppTil5000MeldingerSomIkkeErSendt() {
        // language=sql
        return database.query(""+
                        " SELECT *" +
                        " FROM AKTIVITET" +
                        " where PORTEFOLJE_KAFKA_OFFSET IS NULL" +
                        " order by VERSJON " +
                        " FETCH NEXT 5000 ROWS ONLY",
                KafkaAktivitetDAO::mapKafkaAktivitetMeldingV4
        );
    }

    @Timed
    public void insertMeldingSendtPaaKafka(KafkaAktivitetMeldingV3 meldingV3, Long offset) {
        // language=sql
        database.update("" +
                        " insert into AKTIVITET_SENDT_PAA_KAFKA_V3 " +
                        " (aktivitet_id, aktivitet_versjon, sendt, offset) " +
                        " values ( ?,?, CURRENT_TIMESTAMP, ? )",
                meldingV3.getAktivitetId(), meldingV3.getVersion(), offset);
    }

    @Timed
    public void updateSendtPaKafka(Long versjon, Long kafkaOffset) {
        // language=sql
        database.update("" +
                        " update AKTIVITET " +
                        " set PORTEFOLJE_KAFKA_OFFSET = ?" +
                        " where VERSJON = ?",
                kafkaOffset, versjon);
    }

    private static KafkaAktivitetMeldingV3 mapKafkaAktivitetMeldingV3(ResultSet rs) throws SQLException {

        AktivitetTypeData aktivitet_type_kode = AktivitetTypeData.valueOf(rs.getString("aktivitet_type_kode"));
        AktivitetStatus status = EnumUtils.valueOf(AktivitetStatus.class, rs.getString("livslopstatus_kode"));

        return KafkaAktivitetMeldingV3
                .builder()
                .aktorId(rs.getString("AKTOR_ID"))
                .aktivitetId(rs.getString("ID"))
                .version(rs.getLong("VERSJON"))
                .fraDato(rs.getDate("FRA_DATO"))
                .tilDato(rs.getDate("TIL_DATO"))
                .endretDato(rs.getDate("ENDRET_DATO"))
                .aktivitetType(typeMap.get(aktivitet_type_kode))
                .aktivitetStatus(status)
                .avtalt(rs.getBoolean("AVTALT"))
                .historisk(rs.getDate("HISTORISK_DATO") != null)
                .build();

    }

    public static KafkaAktivitetMeldingV4 mapKafkaAktivitetMeldingV4(ResultSet rs) throws SQLException {
        AktivitetTypeDTO typeDTO = typeMap.get(AktivitetTypeData.valueOf(rs.getString("aktivitet_type_kode")));
        AktivitetStatus status = EnumUtils.valueOf(AktivitetStatus.class, rs.getString("livslopstatus_kode"));
        InnsenderData lagt_inn_av = EnumUtils.valueOf(InnsenderData.class, rs.getString("lagt_inn_av"));
        EndringsType transaksjons_type = EndringsType.get(EnumUtils.valueOf(AktivitetTransaksjonsType.class, rs.getString("transaksjons_type")));

        return KafkaAktivitetMeldingV4.builder()
                .aktivitetId(String.valueOf(rs.getLong("aktivitet_id")))
                .version(rs.getLong("versjon"))
                .aktorId(rs.getString("aktor_id"))
                .fraDato(Database.hentDato(rs, "fra_dato"))
                .tilDato(Database.hentDato(rs, "til_dato"))
                .endretDato(Database.hentDato(rs, "endret_dato"))
                .aktivitetType(typeDTO)
                .aktivitetStatus(status)
                .lagtInnAv(lagt_inn_av)
                .endringsType(transaksjons_type)
                .avtalt(rs.getBoolean("avtalt"))
                .historisk(rs.getTimestamp( "historisk_dato") != null)
                .build();
    }
}
