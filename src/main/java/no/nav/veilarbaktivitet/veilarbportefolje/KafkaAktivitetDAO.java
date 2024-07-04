package no.nav.veilarbaktivitet.veilarbportefolje;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.Helpers;
import no.nav.veilarbaktivitet.aktivitetskort.dto.AktivitetskortType;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.util.EnumUtils;
import no.nav.veilarbaktivitet.veilarbdbutil.VeilarbAktivitetResultSet;
import no.nav.veilarbaktivitet.veilarbportefolje.dto.KafkaAktivitetMeldingV4;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static no.nav.veilarbaktivitet.veilarbportefolje.dto.AktivitetTypeDTO.aktivitetsKortTypeToArenaTiltakskode;

@Repository
@AllArgsConstructor
public class KafkaAktivitetDAO {

    private final Database database;

    @Timed
    public List<KafkaAktivitetMeldingV4> hentOppTil5000MeldingerSomIkkeErSendtPaAiven() {
        return database.getJdbcTemplate().query(
                """ 
                        SELECT SFN.AKTIVITET_ID AS SFN_KEY, SFN.SVARFRIST, SFN.CV_KAN_DELES,
                        EA.AKTIVITET_ID AS EA_KEY, EA.TILTAK_KODE, EA.ARENA_ID, EA.AKTIVITETKORT_TYPE,
                        A.* FROM AKTIVITET A
                        LEFT JOIN STILLING_FRA_NAV SFN ON A.AKTIVITET_ID = SFN.AKTIVITET_ID AND A.VERSJON = SFN.VERSJON
                        LEFT JOIN EKSTERNAKTIVITET EA on A.AKTIVITET_ID = EA.AKTIVITET_ID and A.VERSJON = EA.VERSJON 
                        WHERE A.PORTEFOLJE_KAFKA_OFFSET_AIVEN IS NULL
                        AND (EA.OPPRETTET_SOM_HISTORISK != 1 OR EA.OPPRETTET_SOM_HISTORISK IS NULL)
                        AND (EA.AKTIVITETKORT_TYPE IS NULL OR EA.AKTIVITETKORT_TYPE != 'ARENA_TILTAK')
                        ORDER BY A.VERSJON
                        FETCH NEXT 5000 ROWS ONLY
                        """,
                KafkaAktivitetDAO::mapKafkaAktivitetMeldingV4
        );
    }

    @Timed
    public void updateSendtPaKafkaAven(Long versjon, Long kafkaOffset) {
        // language=sql
        database.update("""
                 update AKTIVITET
                 set PORTEFOLJE_KAFKA_OFFSET_AIVEN = ?
                 where VERSJON = ?
                 """,
                kafkaOffset, versjon);
    }

    public static KafkaAktivitetMeldingV4 mapKafkaAktivitetMeldingV4(ResultSet resultSet, int i) throws SQLException {
        var rs = new VeilarbAktivitetResultSet(resultSet);
        AktivitetTypeDTO domainAktivitetType = Helpers.Type.getDTO(AktivitetTypeData.valueOf(rs.getString("aktivitet_type_kode")));
        var aktivitetTypeDto = no.nav.veilarbaktivitet.veilarbportefolje.dto.AktivitetTypeDTO.fromDomainAktivitetType(domainAktivitetType);
        var aktivitetsId = String.valueOf(rs.getLong("aktivitet_id"));
        String tiltakskode = null;
        // Eksterne aktiviteter START
        if (AktivitetTypeDTO.EKSTERNAKTIVITET.equals(domainAktivitetType)) {
            AktivitetskortType aktivitetskortType = EnumUtils.valueOf(AktivitetskortType.class, rs.getString("aktivitetkort_type"));
            tiltakskode = rs.getString("TILTAK_KODE");
            if (tiltakskode == null) {
                tiltakskode = aktivitetsKortTypeToArenaTiltakskode(aktivitetskortType);
            }
            var arenaId = rs.getString("ARENA_ID");
            if (arenaId != null) {
                aktivitetsId = String.valueOf(arenaId); // Usikker på om dette er lurt. Snakk med OBO før vi begynner å sende arenatiltak deres vei.
            }
        }
        // Eksterne aktiviteter SLUTT
        AktivitetStatus status = EnumUtils.valueOf(AktivitetStatus.class, rs.getString("livslopstatus_kode"));
        Innsender lagtInnAv = EnumUtils.valueOf(Innsender.class, rs.getString("lagt_inn_av"));
        EndringsType transaksjonsType = EndringsType.get(EnumUtils.valueOf(AktivitetTransaksjonsType.class, rs.getString("transaksjons_type")));
        StillingFraNavPortefoljeData stillingFraNavData =
                StillingFraNavPortefoljeData.hvisStillingFraNavDataFinnes(
                        rs.getObject("sfn_key"),
                        rs.getBooleanOrNull("CV_KAN_DELES"),
                        rs.getDate("SVARFRIST")
                );


        return KafkaAktivitetMeldingV4.builder()
                .aktivitetId(aktivitetsId)
                .version(rs.getLong("versjon"))
                .aktorId(rs.getString("aktor_id"))
                .fraDato(Database.hentDato(rs, "fra_dato"))
                .tilDato(Database.hentDato(rs, "til_dato"))
                .endretDato(Database.hentDato(rs, "endret_dato"))
                .aktivitetType(aktivitetTypeDto)
                .aktivitetStatus(status)
                .lagtInnAv(lagtInnAv)
                .endringsType(transaksjonsType)
                .stillingFraNavData(stillingFraNavData)
                .avtalt(rs.getBoolean("avtalt"))
                .historisk(rs.getTimestamp("historisk_dato") != null)
                .tiltakskode(tiltakskode)
                .build();
    }
}
