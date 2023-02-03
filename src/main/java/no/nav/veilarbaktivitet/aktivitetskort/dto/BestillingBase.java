package no.nav.veilarbaktivitet.aktivitetskort.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import no.nav.veilarbaktivitet.aktivitetskort.ActionType;
import no.nav.veilarbaktivitet.aktivitetskort.dto.kassering.KasseringsBestilling;

import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = PROPERTY,
    property = "actionType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = KafkaAktivitetskortWrapperDTO.class, name = "UPSERT_AKTIVITETSKORT_V1"),
    @JsonSubTypes.Type(value = KasseringsBestilling.class, name = "KASSER_AKTIVITET"),
})
@Getter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public abstract class BestillingBase {
    protected String source;
    protected UUID messageId;
    protected ActionType actionType;

    protected BestillingBase(String source, UUID messageId, ActionType actionType) {
        this.source = source;
        this.messageId = messageId;
        this.actionType = actionType;
    }

    public abstract UUID getAktivitetskortId();
}
