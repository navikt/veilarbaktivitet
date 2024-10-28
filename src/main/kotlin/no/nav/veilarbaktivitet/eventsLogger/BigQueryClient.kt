package no.nav.veilarbaktivitet.eventsLogger

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.TableId
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.ZonedDateTime

enum class EventType {
    SAMTALEREFERAT_OPPRETTET,
    SAMTALEREFERAT_DELT_MED_BRUKER,
}

data class SamtalerefereratPublisertFordeling(
    val antallPublisert: Int,
    val antallIkkePublisert: Int,
)

interface BigQueryClient {
    fun logEvent(aktivitetData: AktivitetData, eventType: EventType)
    fun logSamtalereferatFordeling(samtalerefereratPublisertFordeling: SamtalerefereratPublisertFordeling)
}

@Service
class BigQueryClientImplementation(@Value("\${gcp.projectId}") val projectId: String): BigQueryClient {
    val SAMTALEREFERAT_EVENTS = "SAMTALEREFERAT_EVENTS"
    val SAMTALEREFERAT_FORDELING = "SAMTALEREFERAT_FORDELING"
    val DATASET_NAME = "aktivitet_metrikker"
    val moteEventsTable = TableId.of(DATASET_NAME, SAMTALEREFERAT_EVENTS)
    val samtalereferatFordelingTable = TableId.of(DATASET_NAME, SAMTALEREFERAT_FORDELING)

    val bigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).build().service
    val log = LoggerFactory.getLogger(BigQueryClient::class.java)

    override fun logEvent(aktivitetData: AktivitetData, eventType: EventType) {
        val moteRow = mapOf(
            "aktivitetId" to aktivitetData.id,
            "event" to eventType.name,
            "versjon" to aktivitetData.versjon,
            "endretDato" to ZonedDateTime.ofInstant(aktivitetData.endretDato.toInstant(), ZoneOffset.systemDefault()).toOffsetDateTime().toString(),
            "erPublisert" to aktivitetData.moteData.isReferatPublisert,
            "opprettet" to ZonedDateTime.ofInstant(aktivitetData.opprettetDato.toInstant(), ZoneOffset.systemDefault()).toOffsetDateTime().toString(),
        )
        val moteEvent = InsertAllRequest.newBuilder(moteEventsTable)
            .addRow(moteRow).build()
        insertWhileToleratingErrors(moteEvent)
    }

    override fun logSamtalereferatFordeling(samtalerefereratPublisertFordeling: SamtalerefereratPublisertFordeling) {
        val fordelingRow = mapOf(
            "antallPublisert" to samtalerefereratPublisertFordeling.antallPublisert,
            "antallIkkePublisert" to samtalerefereratPublisertFordeling.antallIkkePublisert,
            "timestamp" to ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime().toString(),
        )
        val fordelingInsertRequest = InsertAllRequest.newBuilder(samtalereferatFordelingTable).addRow(fordelingRow).build()
        insertWhileToleratingErrors(fordelingInsertRequest)
    }

    fun insertWhileToleratingErrors(insertRequest: InsertAllRequest) {
        kotlin.runCatching {
            val response = bigQuery.insertAll(insertRequest)
            val errors = response.insertErrors
            if (errors.isNotEmpty()) {
                log.error("Error inserting bigquery rows", errors)
            }
        }.onFailure {
            log.error("BigQuery error", it)
        }
    }


}