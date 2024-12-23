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
    SAMTALEREFERAT_OPPRETTET, // TODO: Endre navn, dette er referat opprettet, ikke aktivitet med type samtalereferat
    SAMTALEREFERAT_FIKK_INNHOLD,
    SAMTALEREFERAT_DELT_MED_BRUKER,
    SAMTALEREFERAT_OPPRETTET_OG_DELT_MED_BRUKER,
}

data class SamtalereferatPublisertFordeling(
    val antallPublisert: Int,
    val antallIkkePublisert: Int,
)

interface BigQueryClient {
    fun logEvent(aktivitetData: AktivitetData, eventType: EventType)
    fun logSamtalereferatFordeling(samtalereferatPublisertFordeling: SamtalereferatPublisertFordeling)
}

@Service
class BigQueryClientImplementation(@Value("\${gcp.projectId}") val projectId: String): BigQueryClient {
    val SAMTALEREFERAT_EVENTS = "SAMTALEREFERAT_EVENTS"
    val SAMTALEREFERAT_FORDELING = "SAMTALEREFERAT_FORDELING"
    val DATASET_NAME = "aktivitet_metrikker"
    val moteEventsTable = TableId.of(DATASET_NAME, SAMTALEREFERAT_EVENTS)
    val samtalereferatFordelingTable = TableId.of(DATASET_NAME, SAMTALEREFERAT_FORDELING)

    fun TableId.insertRequest(row: Map<String, Any>): InsertAllRequest {
        return InsertAllRequest.newBuilder(this).addRow(row).build()
    }

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
            "aktivitetsType" to aktivitetData.aktivitetType.name,
            "referatLengde" to (aktivitetData.moteData?.referat?.length ?: -1)
        )
        val moteEvent = moteEventsTable.insertRequest(moteRow)
        insertWhileToleratingErrors(moteEvent)
    }

    override fun logSamtalereferatFordeling(samtalereferatPublisertFordeling: SamtalereferatPublisertFordeling) {
        val fordelingRow = mapOf(
            "antallPublisert" to samtalereferatPublisertFordeling.antallPublisert,
            "antallIkkePublisert" to samtalereferatPublisertFordeling.antallIkkePublisert,
            "timestamp" to ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime().toString(),
        )
        val fordelingInsertRequest = samtalereferatFordelingTable.insertRequest(fordelingRow)
        insertWhileToleratingErrors(fordelingInsertRequest)
    }

    private fun insertWhileToleratingErrors(insertRequest: InsertAllRequest) {
        runCatching {
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
