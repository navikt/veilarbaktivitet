package no.nav.veilarbaktivitet.oppfolging.periode

import no.nav.veilarbaktivitet.person.Person.AktorId
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Types
import java.time.ZoneOffset
import java.util.*

@Repository
open class OppfolgingsperiodeDAO(val jdbc: NamedParameterJdbcTemplate) {

    val log = LoggerFactory.getLogger(javaClass)

    open fun upsertOppfolgingsperide(oppfolgingsperiode: Oppfolgingsperiode) {
        val params = mapOf(
            "aktorId" to oppfolgingsperiode.aktorid,
            "id" to oppfolgingsperiode.oppfolgingsperiodeId.toString(),
            "fra" to oppfolgingsperiode.startTid.toOffsetDateTime(),
            "til" to oppfolgingsperiode.sluttTid?.toOffsetDateTime()
        )
        jdbc.update(
            """
                INSERT INTO OPPFOLGINGSPERIODE (aktorId, id, fra, til)
                VALUES (:aktorId, :id, :fra, :til)
                ON CONFLICT (id) 
                DO UPDATE SET til = :til, updated = current_timestamp;
                """.trimIndent(), params
        )

        log.info("oppfølgingsperiodemelding behandlet {}", oppfolgingsperiode)

    }

    open fun getByAktorId(aktorId: AktorId ): List<Oppfolgingsperiode> {
        val params = MapSqlParameterSource()
            .addValue("aktorId", aktorId.get(), Types.VARCHAR)
        val sql = """
            SELECT * FROM oppfolgingsperiode WHERE aktorId = :aktorId ORDER BY coalesce(til, TO_DATE('9999-12-31', 'YYYY-MM-DD')) DESC
        """.trimIndent()
        return jdbc.query(sql, params) {row, _ ->
            Oppfolgingsperiode(
                aktorId.get(),
                UUID.fromString(row.getString("id")),
                row.getTimestamp("fra").toLocalDateTime().atZone(ZoneOffset.systemDefault()),
                row.getTimestamp("til")?.toLocalDateTime()?.atZone(ZoneOffset.systemDefault()),
            )
        }
    }
}
