package no.nav.veilarbaktivitet.eventsLogger

import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.TableId
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.util.DateUtils
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

    override fun logEvent(aktivitetData: AktivitetData, eventType: EventType) {
        val bigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).build().service
        val moteRow = mapOf(
            "id" to aktivitetData.id,
            "event" to eventType.name,
            "erPublisert" to aktivitetData.moteData.isReferatPublisert,
            "opprettet" to DateUtils.dateToZonedDateTime(aktivitetData.opprettetDato).toString(),
        )
        val moteEvent = InsertAllRequest.newBuilder(moteEventsTable)
            .addRow(moteRow).build()
        bigQuery.insertAll(moteEvent)
    }
}