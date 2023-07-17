package no.nav.veilarbaktivitet.aktivitetskort

import lombok.ToString
import no.nav.common.kafka.consumer.ConsumeStatus
import no.nav.common.kafka.consumer.TopicConsumer
import no.nav.veilarbaktivitet.aktivitet.MetricService
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling
import no.nav.veilarbaktivitet.aktivitetskort.dto.BestillingBase
import no.nav.veilarbaktivitet.aktivitetskort.feil.*
import no.nav.veilarbaktivitet.aktivitetskort.service.AktivitetskortService
import no.nav.veilarbaktivitet.aktivitetskort.service.KasseringsService
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
open class AktivitetskortConsumer (
    private val aktivitetskortService: AktivitetskortService,
    private val kasseringsService: KasseringsService,
    private val feilProducer: AktivitetsKortFeilProducer,
    private val aktivitetskortMetrikker: AktivitetskortMetrikker,
    private val bestillingsCreator: AktivitetsbestillingCreator,
) : TopicConsumer<String, String?>  {

    val log = LoggerFactory.getLogger(javaClass)
    @Transactional(noRollbackFor = [AktivitetsKortFunksjonellException::class])
    override fun consume(consumerRecord: ConsumerRecord<String, String?>): ConsumeStatus {
        var messageId: UUID? = null
        return try {
            messageId = extractMessageId(consumerRecord)
            val funksjonellId = UUID.fromString(consumerRecord.key()) // Lik aktivitetskort.id i payload
            ignorerHvisSettFor(messageId, funksjonellId)
            if (messageId == funksjonellId) {
                throw MessageIdIkkeUnikFeil(
                    ErrorMessage("messageId må være unik for hver melding. aktivitetsId er lik messageId"),
                    null
                )
            }
            val bestilling = bestillingsCreator.lagBestilling(consumerRecord)
            if (bestilling.getMessageId() == null) {
                bestilling.setMessageId(messageId) // messageId populert i header i stedet for payload
            }
            MDC.put(MetricService.SOURCE, bestilling.getSource())
            val timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(consumerRecord.timestamp()), ZoneId.systemDefault())
            log.info(
                "Konsumerer aktivitetskortmelding: offset={}, partition={}, messageId={}, sendt={}, funksjonellId={}",
                consumerRecord.offset(),
                consumerRecord.partition(),
                bestilling.getMessageId(),
                timestamp,
                bestilling.aktivitetskortId
            )
            behandleBestilling(bestilling)
        } catch (e: DuplikatMeldingFeil) {
            ConsumeStatus.OK
        } catch (e: AktivitetsKortFunksjonellException) {
            log.error(
                "Funksjonell feil {} i aktivitetkortConumer for aktivitetskort_v1 offset={} partition={}",
                e.message,
                consumerRecord.offset(),
                consumerRecord.partition()
            )
            if (messageId == null) {
                log.error(
                    "MessageId mangler for aktivitetskort melding med key {}. Får ikke oppdatert meldingsresultat.",
                    consumerRecord.key()
                )
            } else {
                aktivitetskortService.oppdaterMeldingResultat(
                    messageId,
                    UpsertActionResult.FUNKSJONELL_FEIL,
                    e.javaClass.simpleName
                )
            }
            feilProducer.publishAktivitetsFeil(e, consumerRecord)
            ConsumeStatus.OK
        } catch (e: Exception) {
            aktivitetskortMetrikker.countAktivitetskortTekniskFeil()
            throw e
        } finally {
            MDC.remove(MetricService.SOURCE)
        }
    }


    @Throws(AktivitetsKortFunksjonellException::class)
    fun behandleBestilling(bestilling: BestillingBase): ConsumeStatus {
        if (bestilling is AktivitetskortBestilling) {
            val upsertActionResult = aktivitetskortService.upsertAktivitetskort(bestilling)
            aktivitetskortService.oppdaterMeldingResultat(bestilling.getMessageId(), upsertActionResult, null)
            aktivitetskortMetrikker.countAktivitetskortUpsert(bestilling, upsertActionResult)
        } else if (bestilling is KasseringsBestilling) {
            kasseringsService.kassertAktivitet(bestilling)
            aktivitetskortService.oppdaterMeldingResultat(bestilling.getMessageId(), UpsertActionResult.KASSER, null)
        } else {
            throw NotImplementedException("Unknown kafka message")
        }
        return ConsumeStatus.OK
    }

    @Throws(DuplikatMeldingFeil::class)
    private fun ignorerHvisSettFor(messageId: UUID?, funksjonellId: UUID) =
        if (aktivitetskortService.harSettMelding(messageId!!)) {
            log.warn("Previously handled message seen {} , ignoring", messageId)
            throw DuplikatMeldingFeil()
        } else {
            aktivitetskortService.lagreMeldingsId(messageId, funksjonellId)
        }

    @Throws(AktivitetsKortFunksjonellException::class)
    private fun extractMessageId(consumerRecord: ConsumerRecord<String, String?>): UUID {
        try {
            JsonUtil.extractStringPropertyFromJson(UNIQUE_MESSAGE_IDENTIFIER, consumerRecord.value())
                ?.let { return UUID.fromString(it) }
            consumerRecord.headers().lastHeader(UNIQUE_MESSAGE_IDENTIFIER)
                ?.let { return UUID.fromString(String(it.value())) }
            throw RuntimeException("Mangler påkrevet messageId på aktivitetskort melding")
        } catch (e: IOException) {
            throw DeserialiseringsFeil(ErrorMessage("Meldingspayload er ikke gyldig json"), e)
        } catch (e: IllegalArgumentException) {
            throw DeserialiseringsFeil(ErrorMessage("MessageId er ikke en gyldig UUID verdi"), e)
        }
    }

    companion object {
        const val UNIQUE_MESSAGE_IDENTIFIER = "messageId"
    }
}
