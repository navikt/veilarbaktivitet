package no.nav.veilarbaktivitet.aktivitetskort.service;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDataRowMapper;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TiltakMigreringDAO {

    private final NamedParameterJdbcTemplate jdbcTemplate;


    private final RowMapper<AktivitetData> rowMapper = (rs, i) -> AktivitetDataRowMapper.mapAktivitet(rs);

    public List<AktivitetData> hentTiltakOpprettetSomHistoriskSomIkkeErHistorisk(int maxAntall) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maxAntall", maxAntall);

        return jdbcTemplate.query("""
                SELECT e.DETALJER AS "EKSTERNAKTIVITET.DETALJER",
                *
                FROM EKSTERNAKTIVITET e JOIN AKTIVITET a ON e.AKTIVITET_ID = a.AKTIVITET_ID AND e.VERSJON = a.VERSJON
                WHERE e.OPPRETTET_SOM_HISTORISK = 1 AND
                    a.GJELDENDE = 1 AND
                    a.HISTORISK_DATO is null
                FETCH FIRST :maxAntall ROWS ONLY
                """, params, rowMapper);
    }


}
