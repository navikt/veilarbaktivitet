package no.nav.veilarbaktivitet.veilarbportefolje;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.Helpers;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.person.InnsenderData;
import no.nav.veilarbaktivitet.util.EnumUtils;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@AllArgsConstructor
public class KafkaAktivitetDAO {
    private final Database database;

    @Timed
    public List<KafkaAktivitetMeldingV4> hentOppTil5000MeldingerSomIkkeErSendtPaAiven() {
        // language=sql
        return database.query("" +
                        " SELECT *" +
                        " FROM AKTIVITET" +
                        " where AKTIVITET.PORTEFOLJE_KAFKA_OFFSET_AIVEN IS NULL" +
                        " order by VERSJON " +
                        " FETCH NEXT 5000 ROWS ONLY",
                KafkaAktivitetDAO::mapKafkaAktivitetMeldingV4
        );
    }

    @Timed
    public void updateSendtPaKafkaAven(Long versjon, Long kafkaOffset) {
        // language=sql
        database.update("" +
                        " update AKTIVITET " +
                        " set PORTEFOLJE_KAFKA_OFFSET_AIVEN = ?" +
                        " where VERSJON = ?",
                kafkaOffset, versjon);
    }

    public static KafkaAktivitetMeldingV4 mapKafkaAktivitetMeldingV4(ResultSet rs) throws SQLException {
        AktivitetTypeDTO typeDTO = Helpers.Type.getDTO(AktivitetTypeData.valueOf(rs.getString("aktivitet_type_kode")));
        AktivitetStatus status = EnumUtils.valueOf(AktivitetStatus.class, rs.getString("livslopstatus_kode"));
        InnsenderData lagtInnAv = EnumUtils.valueOf(InnsenderData.class, rs.getString("lagt_inn_av"));
        EndringsType transaksjonsType = EndringsType.get(EnumUtils.valueOf(AktivitetTransaksjonsType.class, rs.getString("transaksjons_type")));

        return KafkaAktivitetMeldingV4.builder()
                .aktivitetId(String.valueOf(rs.getLong("aktivitet_id")))
                .version(rs.getLong("versjon"))
                .aktorId(rs.getString("aktor_id"))
                .fraDato(Database.hentDato(rs, "fra_dato"))
                .tilDato(Database.hentDato(rs, "til_dato"))
                .endretDato(Database.hentDato(rs, "endret_dato"))
                .aktivitetType(typeDTO)
                .aktivitetStatus(status)
                .lagtInnAv(lagtInnAv)
                .endringsType(transaksjonsType)
                .avtalt(rs.getBoolean("avtalt"))
                .historisk(rs.getTimestamp("historisk_dato") != null)
                .build();
    }
}
