package no.nav.veilarbaktivitet.eventsLogger

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.TableId
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.util.DateUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

enum class EventType {
    SAMTALEREFERAT_OPPRETTET,
    SAMTALEREFERAT_DELT_MED_BRUKER,
}

interface BigQueryClient {
    fun logEvent(aktivitetData: AktivitetData, eventType: EventType)
}

@Service
class BigQueryClientImplementation(@Value("\${gcp.projectId}") val projectId: String): BigQueryClient {
    val SAMTALEREFERAT_EVENTS = "SAMTALEREFERAT_EVENTS"
    val DATASET_NAME = "aktivitet_metrikker"
    val moteEventsTable = TableId.of(DATASET_NAME, SAMTALEREFERAT_EVENTS)

    val log = LoggerFactory.getLogger(BigQueryClient::class.java)

    override fun logEvent(aktivitetData: AktivitetData, eventType: EventType) {
        val bigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).build().service
        val moteRow = mapOf(
            "aktivitetId" to aktivitetData.id,
            "event" to eventType.name,
            "versjon" to aktivitetData.versjon,
            "endretDato" to DateUtils.dateToLocalDateTime(aktivitetData.endretDato).toString(),
            "erPublisert" to aktivitetData.moteData.isReferatPublisert,
            "opprettet" to DateUtils.dateToLocalDateTime(aktivitetData.opprettetDato).toString(),
        )
        val moteEvent = InsertAllRequest.newBuilder(moteEventsTable)
            .addRow(moteRow).build()
        val response = bigQuery.insertAll(moteEvent)
        val errors = response.insertErrors
        if (errors.isNotEmpty()) {
            log.error("Error inserting bigquery rows", errors)
        }
    }
}