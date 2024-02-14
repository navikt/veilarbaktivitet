package no.nav.veilarbaktivitet.aktivitetskort.service

import lombok.RequiredArgsConstructor
import no.nav.veilarbaktivitet.aktivitet.AktivitetDataRowMapper
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Service
import java.sql.ResultSet

@Service
@RequiredArgsConstructor
class TiltakMigreringDAO (
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val rowMapper = RowMapper { rs: ResultSet, _: Int -> AktivitetDataRowMapper.mapAktivitet(rs) }
    fun hentTiltakOpprettetSomHistoriskSomIkkeErHistorisk(maxAntall: Int): List<AktivitetData> {
        val params = MapSqlParameterSource()
            .addValue("maxAntall", maxAntall)
        // EKSTERNAKTIVITET.DETALJER noedvendig pga AktivitetDataRowMapper
        return jdbcTemplate.query(
            """
                SELECT e.DETALJER AS "EKSTERNAKTIVITET.DETALJER",
                A.*, E.*
                FROM EKSTERNAKTIVITET e JOIN AKTIVITET a ON e.AKTIVITET_ID = a.AKTIVITET_ID AND e.VERSJON = a.VERSJON
                WHERE e.OPPRETTET_SOM_HISTORISK = 1 AND
                    a.GJELDENDE = 1 AND
                    a.HISTORISK_DATO is null
                FETCH FIRST :maxAntall ROWS ONLY
                
                """.trimIndent(), params, rowMapper
        )
    }

    fun flyttFHOTilAktivitet(fhoId: String, aktivitetId: Long) {
        val params: SqlParameterSource = MapSqlParameterSource()
            .addValue("fhoId", fhoId)
            .addValue("aktivitetId", aktivitetId)
        val updated = jdbcTemplate.update(
            """
            UPDATE FORHAANDSORIENTERING SET AKTIVITET_ID = :aktivitetId
            WHERE ID = :fhoId
        """.trimIndent(), params)

        if (updated == 0) return

        jdbcTemplate.update("""
            UPDATE AKTIVITET SET FHO_ID = :fhoId WHERE AKTIVITET_ID = :aktivitetId AND GJELDENDE = 1
        """.trimIndent(), params)

        log.debug("La til teknisk id på FHO med id={}, tekniskId={}", fhoId, aktivitetId)
    }
}
