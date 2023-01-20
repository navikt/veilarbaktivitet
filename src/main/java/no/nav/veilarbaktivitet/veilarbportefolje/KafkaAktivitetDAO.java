package no.nav.veilarbaktivitet.veilarbportefolje;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.Helpers;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;
import no.nav.veilarbaktivitet.config.database.Database;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.util.EnumUtils;
import no.nav.veilarbaktivitet.veilarbportefolje.dto.KafkaAktivitetMeldingV4;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@AllArgsConstructor
public class KafkaAktivitetDAO {
    public static final String TILTAKSKODE_VARIG_LONNSTILSKUDD = "VARLONTIL";
    public static final String TILTAKSKODE_MIDLERTIDIG_LONNSTILSKUDD = "MIDLONTIL";
    private final Database database;

    @Timed
    public List<KafkaAktivitetMeldingV4> hentOppTil5000MeldingerSomIkkeErSendtPaAiven(boolean skalBehandleEksterneAktiviteter) {
        if (skalBehandleEksterneAktiviteter) {
            return database.getJdbcTemplate().query(
                    """ 
                            SELECT SFN.AKTIVITET_ID AS SFN_KEY, SFN.SVARFRIST, SFN.CV_KAN_DELES,
                            EA.AKTIVITET_ID AS EA_KEY, EA.TILTAK_KODE, EA.ARENA_ID, EA.AKTIVITETKORT_TYPE,
                            A.* FROM AKTIVITET A
                            LEFT JOIN STILLING_FRA_NAV SFN ON A.AKTIVITET_ID = SFN.AKTIVITET_ID AND A.VERSJON = SFN.VERSJON
                            LEFT JOIN EKSTERNAKTIVITET EA on A.AKTIVITET_ID = EA.AKTIVITET_ID and A.VERSJON = EA.VERSJON 
                            WHERE A.PORTEFOLJE_KAFKA_OFFSET_AIVEN IS NULL
                            AND (EA.OPPRETTET_SOM_HISTORISK != 1 OR EA.OPPRETTET_SOM_HISTORISK IS NULL)
                            ORDER BY A.VERSJON
                            FETCH NEXT 5000 ROWS ONLY
                            """,
                    KafkaAktivitetDAO::mapKafkaAktivitetMeldingV4
            );
        } else {
            return database.getJdbcTemplate().query(
                    """ 
                            SELECT SFN.AKTIVITET_ID AS SFN_KEY, SFN.SVARFRIST, SFN.CV_KAN_DELES,
                            EA.AKTIVITET_ID AS EA_KEY, EA.TILTAK_KODE, EA.ARENA_ID, EA.AKTIVITETKORT_TYPE,
                            A.* FROM AKTIVITET A
                            LEFT JOIN STILLING_FRA_NAV SFN ON A.AKTIVITET_ID = SFN.AKTIVITET_ID AND A.VERSJON = SFN.VERSJON
                            LEFT JOIN EKSTERNAKTIVITET EA on A.AKTIVITET_ID = EA.AKTIVITET_ID and A.VERSJON = EA.VERSJON 
                            WHERE A.PORTEFOLJE_KAFKA_OFFSET_AIVEN IS NULL
                            AND (EA.OPPRETTET_SOM_HISTORISK != 1 OR EA.OPPRETTET_SOM_HISTORISK IS NULL)
                            AND A.AKTIVITET_TYPE_KODE != 'EKSTERNAKTIVITET'
                            ORDER BY A.VERSJON
                            FETCH NEXT 5000 ROWS ONLY
                            """,
                    KafkaAktivitetDAO::mapKafkaAktivitetMeldingV4
            );
        }
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

    public static KafkaAktivitetMeldingV4 mapKafkaAktivitetMeldingV4(ResultSet rs, int i) throws SQLException {
        AktivitetTypeDTO domainAktivitetType = Helpers.Type.getDTO(AktivitetTypeData.valueOf(rs.getString("aktivitet_type_kode")));
        // Eksterne aktiviteter START
        AktivitetskortType aktivitetskortType = EnumUtils.valueOf(AktivitetskortType.class, rs.getString("aktivitetkort_type"));
        var aktivitetTypeDto = no.nav.veilarbaktivitet.veilarbportefolje.dto.AktivitetTypeDTO.fromDomainAktivitetType(domainAktivitetType,aktivitetskortType);

        var tiltakskode = rs.getString("TILTAK_KODE");
        if (AktivitetTypeDTO.EKSTERNAKTIVITET.equals(domainAktivitetType)) {
            tiltakskode = finnTiltakskode(aktivitetskortType, tiltakskode);
        }
        var arenaId = rs.getString("ARENA_ID");
        // Eksterne aktiviteter SLUTT
        AktivitetStatus status = EnumUtils.valueOf(AktivitetStatus.class, rs.getString("livslopstatus_kode"));
        Innsender lagtInnAv = EnumUtils.valueOf(Innsender.class, rs.getString("lagt_inn_av"));
        EndringsType transaksjonsType = EndringsType.get(EnumUtils.valueOf(AktivitetTransaksjonsType.class, rs.getString("transaksjons_type")));
        StillingFraNavPortefoljeData stillingFraNavData =
                StillingFraNavPortefoljeData.hvisStillingFraNavDataFinnes(
                        rs.getObject("sfn_key"),
                        rs.getObject("CV_KAN_DELES", Boolean.class),
                        rs.getDate("SVARFRIST")
                );


        var aktivitetsId = arenaId != null ? arenaId : String.valueOf(rs.getLong("aktivitet_id"));

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

    private static String finnTiltakskode(AktivitetskortType aktivitetskortType, String tiltakskode)  {
        return switch (aktivitetskortType) {
            case MIDLERTIDIG_LONNSTILSKUDD -> TILTAKSKODE_MIDLERTIDIG_LONNSTILSKUDD;
            case VARIG_LONNSTILSKUDD -> TILTAKSKODE_VARIG_LONNSTILSKUDD;
            case ARENA_TILTAK -> tiltakskode;
        };
    }
}
