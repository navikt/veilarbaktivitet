package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.AllArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDataRowMapper;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class DelingAvCvDAO {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public boolean eksistererDelingAvCv(String bestillingsId) {
        SqlParameterSource bestillingsIdParameter = new MapSqlParameterSource("bestillingsId", bestillingsId);
        // language=sql
        return !jdbcTemplate.queryForList("SELECT BESTILLINGSID FROM STILLING_FRA_NAV WHERE BESTILLINGSID=:bestillingsId ", bestillingsIdParameter, String.class).isEmpty();
    }

    public List<AktivitetData> hentStillingFraNavUtenSvarDerFristErUtlopt(long maxAntall) {
        SqlParameterSource parameter = new MapSqlParameterSource("maxAntall", maxAntall);
        return jdbcTemplate.query("""
                            SELECT SFN.ARBEIDSGIVER as "STILLING_FRA_NAV.ARBEIDSGIVER", SFN.ARBEIDSSTED as "STILLING_FRA_NAV.ARBEIDSSTED",
                            SFN.DETALJER AS "STILLING_FRA_NAV.DETALJER",
                            A.*, SFN.*
                            FROM AKTIVITET A
                            JOIN STILLING_FRA_NAV SFN ON A.AKTIVITET_ID = SFN.AKTIVITET_ID AND A.VERSJON = SFN.VERSJON
                            WHERE AKTIVITET_TYPE_KODE  = 'STILLING_FRA_NAV' 
                            AND LIVSLOPSTATUS_KODE != 'AVBRUTT' 
                            AND GJELDENDE = 1 
                            AND HISTORISK_DATO is null
                            AND SVARFRIST < current_timestamp 
                            AND SFN.CV_KAN_DELES IS NULL
                            order by A.AKTIVITET_ID
                            fetch first :maxAntall rows only 
                        """,
                parameter,
                new AktivitetDataRowMapper());
    }

    public List<AktivitetData> hentStillingFraNavSomErFullfortEllerAvbruttUtenSvar(long maxAntall, long sisteProsesserteVersjon) {
        SqlParameterSource parameter = new MapSqlParameterSource("maxAntall", maxAntall)
                .addValue("sisteProsesserteVersjon", sisteProsesserteVersjon);
        return jdbcTemplate.query("""
                            SELECT SFN.ARBEIDSGIVER as "STILLING_FRA_NAV.ARBEIDSGIVER", SFN.ARBEIDSSTED as "STILLING_FRA_NAV.ARBEIDSSTED",
                            SFN.DETALJER AS "STILLING_FRA_NAV.DETALJER",
                            A.*, SFN.*
                            FROM AKTIVITET A
                            JOIN STILLING_FRA_NAV SFN ON A.AKTIVITET_ID = SFN.AKTIVITET_ID AND A.VERSJON = SFN.VERSJON
                            WHERE AKTIVITET_TYPE_KODE  = 'STILLING_FRA_NAV'
                            AND LIVSLOPSTATUS_KODE IN('AVBRUTT','FULLFORT')
                            AND GJELDENDE = 1
                            AND HISTORISK_DATO is null
                            AND SFN.LIVSLOPSSTATUS NOT IN('AVBRUTT_AV_BRUKER', 'AVBRUTT_AV_SYSTEM', 'HAR_SVART')
                            AND SFN.CV_KAN_DELES IS NULL
                            AND A.versjon > :sisteProsesserteVersjon
                            order by A.versjon
                            limit :maxAntall
                        """,
                parameter,
                new AktivitetDataRowMapper());
    }
}

