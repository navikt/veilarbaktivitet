package no.nav.veilarbaktivitet.aktiviterTilKafka;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import no.nav.veilarbaktivitet.db.Database;
import no.nav.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
    public void insertMeldingSendtPaaKafka(KafkaAktivitetMeldingV3 meldingV3, Long offset) {
        // language=sql
        database.update("" +
                        " insert into AKTIVITET_SENDT_PAA_KAFKA_V3 " +
                        " (aktivitet_id, aktivitet_versjon, sendt, offset) " +
                        " values ( ?,?, CURRENT_TIMESTAMP, ? )",
                meldingV3.getAktivitetId(), meldingV3.getVersion(), offset);
    }

    private static KafkaAktivitetMeldingV3 mapKafkaAktivitetMeldingV3(ResultSet rs) throws SQLException {

        AktivitetTypeData aktivitet_type_kode = AktivitetTypeData.valueOf(rs.getString("aktivitet_type_kode"));
        AktivitetStatus status = EnumUtils.valueOf(AktivitetStatus.class, rs.getString("livslopstatus_kode"));

        return KafkaAktivitetMeldingV3
                .builder()
                .aktorId(rs.getString("AKTOR_ID"))
                .aktivitetId(rs.getString("ID"))
                .version(rs.getString("VERSJON"))
                .fraDato(rs.getDate("FRA_DATO"))
                .tilDato(rs.getDate("TIL_DATO"))
                .endretDato(rs.getDate("ENDRET_DATO"))
                .aktivitetType(typeMap.get(aktivitet_type_kode))
                .aktivitetStatus(status)
                .avtalt(rs.getBoolean("AVTALT"))
                .historisk(rs.getDate("HISTORISK_DATO") != null)
                .build();

    }
}
