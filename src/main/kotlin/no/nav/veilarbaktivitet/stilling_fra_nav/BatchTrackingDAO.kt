package no.nav.veilarbaktivitet.stilling_fra_nav

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

enum class BatchJob {
    Deling_av_cv_avbrutt_eller_fuulfort_uten_svar
}

@Repository
open class BatchTrackingDAO(
    val template: NamedParameterJdbcTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

   open fun setSisteProsesserteVersjon(batch: BatchJob, sisteProsesserteVersjon: Long) {
        template.update("""
            INSERT INTO batch_tracking (batch_name, last_offset) VALUES (:batchName, :sisteProsesserteVersjon)
            ON CONFLICT(batch_name)
            DO UPDATE SET last_offset = :sisteProsesserteVersjon
        """.trimIndent(), mapOf("sisteProsesserteVersjon" to sisteProsesserteVersjon, "batchName" to batch.name))
   }

    open fun hentSisteProsseserteVersjon(batch: BatchJob): Long {
        val results = template.query("""
            SELECT last_offset FROM batch_tracking where batch_name = :batchName
        """.trimIndent(), mapOf("batchName" to batch.name)
        ) { row, i -> row.getLong("last_offset") }
        if (results.isEmpty()) {
            log.warn("Could not find last_offset for batch: ${batch.name} ")
            return 0
        }
        return results.first()
    }
}

sealed class BatchResult(val versjon: Long) {
    class Success(versjon: Long): BatchResult(versjon)
    class Failure(versjon: Long): BatchResult(versjon)
}

fun List<BatchResult>.sisteProsesserteVersjon(fallbackOffset: Long): Long {
    val førsteFeiledeVersjon = this
        .filterIsInstance<BatchResult.Failure>()
        .minByOrNull { it.versjon }
        ?.versjon
    return (førsteFeiledeVersjon?.minus(1)
            ?: this.filterIsInstance<BatchResult.Success>().maxByOrNull { it.versjon }?.versjon)
        ?: fallbackOffset
}
