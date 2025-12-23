package no.nav.veilarbaktivitet.aktivitetskort

import lombok.ToString
import no.nav.common.kafka.consumer.ConsumeStatus
import no.nav.common.kafka.consumer.TopicConsumer
import no.nav.veilarbaktivitet.admin.KasseringsService
import no.nav.veilarbaktivitet.aktivitet.MetricService
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling
import no.nav.veilarbaktivitet.aktivitetskort.dto.BestillingBase
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource
import no.nav.veilarbaktivitet.aktivitetskort.feil.*
import no.nav.veilarbaktivitet.aktivitetskort.service.AktivitetskortService
import no.nav.veilarbaktivitet.aktivitetskort.service.UpsertActionResult
import no.nav.veilarbaktivitet.aktivitetskort.util.JsonUtil
import org.apache.commons.lang3.NotImplementedException
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Service
@ToString(of = ["aktivitetskortService"])
class AktivitetskortConsumer (
    private val aktivitetskortService: AktivitetskortService,
    private val kasseringsService: KasseringsService,
    private val feilProducer: AktivitetsKortFeilProducer,
    private val aktivitetskortMetrikker: AktivitetskortMetrikker,
    private val bestillingsCreator: AktivitetsbestillingCreator,
) : TopicConsumer<String, String>  {

    data class OffsetAndPartition(
        val offset: Long,
        val partition: Int
    )

    val log = LoggerFactory.getLogger(javaClass)
    @Transactional(noRollbackFor = [AktivitetsKortFunksjonellException::class])
    override fun consume(consumerRecord: ConsumerRecord<String, String>): ConsumeStatus {
        var traceFields: TraceFields? = null
        return try {
            traceFields = extractTraceFields(consumerRecord)
            val (messageId) = traceFields
            val funksjonellId = UUID.fromString(consumerRecord.key()) // Lik aktivitetskort.id i payload
            ignorerHvisSettFor(messageId, funksjonellId, OffsetAndPartition(consumerRecord.offset(),  consumerRecord.partition()))
            if (messageId == funksjonellId) {
                throw MessageIdIkkeUnikFeil(
                    ErrorMessage("messageId må være unik for hver melding. aktivitetsId er lik messageId"),
                    null
                )
            }
            val bestilling = bestillingsCreator.lagBestilling(consumerRecord, messageId)

            MDC.put(MetricService.SOURCE, bestilling.source)
            log.info(
                "Konsumerer aktivitetskortmelding: offset={}, partition={}, messageId={}, sendt={}, funksjonellId={}",
                consumerRecord.offset(),
                consumerRecord.partition(),
                bestilling.messageId,
                consumerRecord.timestamp().toLocalDateTimeDefaultZone(),
                bestilling.getAktivitetskortId()
            )
            behandleBestilling(bestilling)
        } catch (e: AktivitetsKortFunksjonellException) {
            val logMedPassendeNivå: (String, String?, Long, Int) -> Unit = when(e) {
                is ManglerOppfolgingsperiodeFeil -> log::warn
                is UlovligEndringFeil -> log::warn
                is DuplikatMeldingFeil -> log::warn
                is ValideringFeil -> log::warn
                else -> log::error
            }
            logMedPassendeNivå(
                "Funksjonell feil {} i aktivitetkortConumer for aktivitetskort_v1 offset={} partition={}",
                e.message,
                consumerRecord.offset(),
                consumerRecord.partition()
            )
            if (traceFields?.messageId == null) {
                log.error(
                    "MessageId mangler for aktivitetskort melding med key {}. Får ikke oppdatert meldingsresultat.",
                    consumerRecord.key()
                )
            } else {
                aktivitetskortService.oppdaterMeldingResultat(
                    traceFields.messageId,
                    UpsertActionResult.FUNKSJONELL_FEIL,
                    e.javaClass.simpleName
                )
            }
            feilProducer.publishAktivitetsFeil(e, consumerRecord, traceFields?.source ?: MessageSource.UNKNOWN)
            ConsumeStatus.OK
        } catch (e: Exception) {
            aktivitetskortMetrikker.countAktivitetskortTekniskFeil(traceFields?.source ?: MessageSource.UNKNOWN)
            // Kotlin treats all exceptions as checked, so we need to wrap it in a RuntimeException.
            // This ensures that the transaction is rolled back.
            throw RuntimeException(e)
        } finally {
            MDC.remove(MetricService.SOURCE)
        }
    }


    @Throws(AktivitetsKortFunksjonellException::class)
    fun behandleBestilling(bestilling: BestillingBase): ConsumeStatus {
        when (bestilling) {
            is AktivitetskortBestilling -> {
                val upsertActionResult = aktivitetskortService.upsertAktivitetskort(bestilling)
                aktivitetskortService.oppdaterMeldingResultat(bestilling.messageId!!, upsertActionResult, null)
                aktivitetskortMetrikker.countAktivitetskortUpsert(bestilling, upsertActionResult)
            }
            is KasseringsBestilling -> {
                kasseringsService.kasserAktivitet(bestilling)
                aktivitetskortService.oppdaterMeldingResultat(bestilling.messageId!!, UpsertActionResult.KASSER, null)
            }
            else -> throw NotImplementedException("Unknown kafka message")
        }
        return ConsumeStatus.OK
    }

    @Throws(DuplikatMeldingFeil::class)
    private fun ignorerHvisSettFor(messageId: UUID, funksjonellId: UUID, offsetAndPartition: OffsetAndPartition) =
        if (aktivitetskortService.harSettMelding(messageId)) {
            log.warn("Previously handled message seen {} , ignoring. Funksjonell id: {}", messageId, funksjonellId)
            throw DuplikatMeldingFeil()
        } else {
            aktivitetskortService.lagreMeldingsId(messageId, funksjonellId, offsetAndPartition)
        }

    @Throws(AktivitetsKortFunksjonellException::class)
    private fun extractTraceFields(consumerRecord: ConsumerRecord<String, String>): TraceFields {
        try {
            val source = JsonUtil.extractStringPropertyFromJson(SOURCE_FIELD_NAME, consumerRecord.value())
            JsonUtil.extractStringPropertyFromJson(UNIQUE_MESSAGE_IDENTIFIER, consumerRecord.value())
                ?.let { return TraceFields(UUID.fromString(it), source) }
            consumerRecord.headers().lastHeader(UNIQUE_MESSAGE_IDENTIFIER)
                ?.let { return TraceFields(UUID.fromString(String(it.value())), source) }
            throw DeserialiseringsFeil(ErrorMessage("Mangler påkrevet messageId på aktivitetskort melding"), java.lang.IllegalArgumentException())
        } catch (e: IOException) {
            throw DeserialiseringsFeil(ErrorMessage("Meldingspayload er ikke gyldig json"), e)
        } catch (e: IllegalArgumentException) {
            throw DeserialiseringsFeil(ErrorMessage("MessageId er ikke en gyldig UUID verdi"), e)
        }
    }

    companion object {
        const val UNIQUE_MESSAGE_IDENTIFIER = "messageId"
        const val SOURCE_FIELD_NAME = "source"
    }
}

data class TraceFields(
    val messageId: UUID,
    val source: MessageSource
) {
    constructor(messageId: UUID, source: String?)
            : this(messageId, MessageSource.entries.find { it.name == source } ?: MessageSource.UNKNOWN)
}

fun Long.toLocalDateTimeDefaultZone() = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())