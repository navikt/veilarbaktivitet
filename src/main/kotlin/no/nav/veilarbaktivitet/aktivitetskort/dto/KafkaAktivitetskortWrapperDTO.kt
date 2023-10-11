package no.nav.veilarbaktivitet.aktivitetskort.dto

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.veilarbaktivitet.aktivitetskort.ActionType
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.MessageSource
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

    constructor(
        payload: Aktivitetskort,
        messageId: UUID = UUID.randomUUID(),
        aktivitetskortType: AktivitetskortType,
        source: MessageSource
    ): this(
        aktivitetskortType,
        payload,
        source.name,
        messageId
    )

    constructor(
        payload: Aktivitetskort,
        type: AktivitetskortType = AktivitetskortType.ARENA_TILTAK,
        source: MessageSource = MessageSource.ARENA_TILTAK_AKTIVITET_ACL
    ): this(payload, UUID.randomUUID(), type, source)
    override fun getAktivitetskortId() = aktivitetskort.id
}
