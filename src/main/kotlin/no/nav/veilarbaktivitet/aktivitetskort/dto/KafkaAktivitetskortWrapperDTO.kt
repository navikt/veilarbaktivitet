package no.nav.veilarbaktivitet.aktivitetskort.dto

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.veilarbaktivitet.aktivitetskort.ActionType
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType
import java.util.*

data class KafkaAktivitetskortWrapperDTO(
    @JsonProperty(required = true)
    var aktivitetskortType: AktivitetskortType,
    @JsonProperty(required = true)
    var aktivitetskort: Aktivitetskort,
    override val source: String,
    override val messageId: UUID?
) : BestillingBase(
    source = source,
    actionType = ActionType.UPSERT_AKTIVITETSKORT_V1,
    messageId = messageId
) {
    override fun getAktivitetskortId() = aktivitetskort.id
}
