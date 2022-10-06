package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
    property = "actionType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = KafkaTiltaksAktivitet.class, name = "UPSERT_TILTAK_AKTIVITET_V1"),
})
@SuperBuilder
@NoArgsConstructor
public abstract class KafkaAktivitetWrapperDTO implements SomethingWithId {
    UUID messageId;
    String source;
    LocalDateTime sendt;
    ActionType actionType;
}

interface SomethingWithId {
    abstract UUID funksjonellId();
}
