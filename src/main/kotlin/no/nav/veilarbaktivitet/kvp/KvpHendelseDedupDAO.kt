package no.nav.veilarbaktivitet.kvp

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class KvpHendelseDedupDAO(
    private val template: NamedParameterJdbcTemplate
) {
    fun reserverDersomNy(dedupKey: String): Boolean {
        val rowsUpdated = template.update(
            """
                INSERT INTO KVP_HENDELSE_PROSESSERT (DEDUP_KEY)
                VALUES (:dedupKey)
                ON CONFLICT (DEDUP_KEY) DO NOTHING
            """.trimIndent(),
            MapSqlParameterSource("dedupKey", dedupKey)
        )

        return rowsUpdated == 1
    }
}
