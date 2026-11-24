package no.nav.veilarbaktivitet.outbox
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

data class OutboxRecord(
    val id: Long,
    val topic: String,
    val key: String,
    val payload: String?,
)

@Repository
open class OutboxDao(
    private val jdbc: NamedParameterJdbcTemplate
) {
    open fun lagre(topic: String, key: String, payload: String?) {
        val sql = """
            INSERT INTO outbox (topic, key, payload)
            VALUES (:topic, :key, :payload)
        """.trimIndent()
        jdbc.update(sql, mapOf("topic" to topic, "key" to key, "payload" to payload))
    }

    open fun hentUsendteMeldinger(event: OutboxService.OutboxMeldingLagretEvent? = null): List<OutboxRecord> {
        val sql = """
            SELECT id, topic, key, payload 
            FROM outbox
            ${if (event != null) "WHERE key = :key AND topic = :topic" else ""}
            ORDER BY opprettet
            FOR UPDATE NOWAIT
            """.trimMargin()
        val params = when {
            event != null -> mapOf("key" to event.key, "topic" to event.topic)
            else -> emptyMap<String, Any>()
        }
        return jdbc.query(sql, params) { rs, _ ->
            OutboxRecord(
                id = rs.getLong("id"),
                topic = rs.getString("topic"),
                key = rs.getString("key"),
                payload = rs.getString("payload"),
            )
        }
    }

    open fun slett(id: Long) {
        jdbc.update("DELETE FROM outbox WHERE id = :id", mapOf("id" to id))
    }
}