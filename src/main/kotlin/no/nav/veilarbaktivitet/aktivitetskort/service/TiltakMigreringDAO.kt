package no.nav.veilarbaktivitet.aktivitetskort.service

import lombok.RequiredArgsConstructor
import no.nav.veilarbaktivitet.aktivitet.AktivitetDataRowMapper
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet

@Service
@RequiredArgsConstructor
class TiltakMigreringDAO (
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {

    private val rowMapper = RowMapper { rs: ResultSet, _: Int -> AktivitetDataRowMapper.mapAktivitet(rs) }
    fun hentTiltakOpprettetSomHistoriskSomIkkeErHistorisk(maxAntall: Int): List<AktivitetData> {
        val params = MapSqlParameterSource()
            .addValue("maxAntall", maxAntall)
        return jdbcTemplate.query(
            """
                SELECT e.DETALJER AS "EKSTERNAKTIVITET.DETALJER",
                *
                FROM EKSTERNAKTIVITET e JOIN AKTIVITET a ON e.AKTIVITET_ID = a.AKTIVITET_ID AND e.VERSJON = a.VERSJON
                WHERE e.OPPRETTET_SOM_HISTORISK = 1 AND
                    a.GJELDENDE = 1 AND
                    a.HISTORISK_DATO is null
                FETCH FIRST :maxAntall ROWS ONLY
                
                """.trimIndent(), params, rowMapper
        )
    }
}
