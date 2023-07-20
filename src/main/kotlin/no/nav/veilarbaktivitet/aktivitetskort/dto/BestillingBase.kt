package no.nav.veilarbaktivitet.aktivitetskort.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.veilarbaktivitet.aktivitetskort.ActionType
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.KasseringsBestilling
import java.util.*

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "actionType")
@JsonSubTypes(
    JsonSubTypes.Type(value = KafkaAktivitetskortWrapperDTO::class, name = "UPSERT_AKTIVITETSKORT_V1"),
    JsonSubTypes.Type(value = KasseringsBestilling::class, name = "KASSER_AKTIVITET")
)
abstract class BestillingBase protected constructor(
    open val source: String,
    open val messageId: UUID?,
    open val actionType: ActionType
) {
//    abstract val aktivitetskortId: UUID?

    abstract fun getAktivitetskortId(): UUID
}
