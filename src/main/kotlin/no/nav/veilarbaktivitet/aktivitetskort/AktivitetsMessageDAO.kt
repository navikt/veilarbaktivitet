package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortConsumer.OffsetAndPartition
import no.nav.veilarbaktivitet.aktivitetskort.service.UpsertActionResult
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class AktivitetsMessageDAO (
    private val template: NamedParameterJdbcTemplate
) {

    fun insert(messageId: UUID, funksjonellId: UUID, offsetAndPartition: OffsetAndPartition) {
        val params = MapSqlParameterSource()
            .addValue("messageId", messageId.toString())
            .addValue("funksjonellId", funksjonellId.toString())
            .addValue("offset", offsetAndPartition.offset)
            .addValue("partition", offsetAndPartition.partition)
        template.update(
            """
                INSERT INTO AKTIVITETSKORT_MSG_ID(MESSAGE_ID, FUNKSJONELL_ID, OFFSET, PARTITION) 
                VALUES (:messageId, :funksjonellId, :offset, :partition)
                
                """.trimIndent(), params
        )
    }

    fun exist(messageId: UUID): Boolean {
        val params = MapSqlParameterSource()
            .addValue("messageId", messageId.toString())
        val antall = template.queryForObject(
            "SELECT COUNT(*) FROM AKTIVITETSKORT_MSG_ID WHERE MESSAGE_ID = :messageId",
            params,
            Int::class.java
        )
        return antall != null && antall > 0
    }

    fun updateActionResult(messageId: UUID, upsertActionResult: UpsertActionResult, reason: String?) {
        val params = MapSqlParameterSource()
            .addValue("messageId", messageId.toString())
            .addValue("actionResult", upsertActionResult.name)
            .addValue("reason", reason)
        template!!.update(
            """
                UPDATE AKTIVITETSKORT_MSG_ID
                SET ACTION_RESULT = :actionResult,
                    REASON = :reason
                WHERE MESSAGE_ID = :messageId
                
                """.trimIndent(), params
        )
    }
}
