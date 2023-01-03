package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.AllArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDataRowMapper;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@AllArgsConstructor
public class RekrutteringsbistandStatusoppdateringDAO {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Optional<AktivitetData> hentAktivitetMedBestillingsId(String bestillingsId) {
        SqlParameterSource bestillingsIdParameter = new MapSqlParameterSource("bestillingsId", bestillingsId);
        // language=sql
        return jdbcTemplate.query("""
                            SELECT SFN.ARBEIDSGIVER as \"STILLING_FRA_NAV.ARBEIDSGIVER\", SFN.ARBEIDSSTED as \"STILLING_FRA_NAV.ARBEIDSSTED\", A.*, SFN.*
                            FROM AKTIVITET A
                            LEFT JOIN STILLING_FRA_NAV SFN ON A.AKTIVITET_ID = SFN.AKTIVITET_ID AND A.VERSJON = SFN.VERSJON
                            WHERE AKTIVITET_TYPE_KODE  = 'STILLING_FRA_NAV' 
                            AND BESTILLINGSID=:bestillingsId 
                            AND GJELDENDE = 1 
                            AND HISTORISK_DATO is null
                            order by A.VERSJON desc
                            fetch first 1 rows only 
                        """,
                bestillingsIdParameter, new AktivitetDataRowMapper()).stream().findFirst();
    }
}
