package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.With;

import java.util.UUID;
@Builder
@With
public class KafkaAktivitetskortWrapperDTO {
    @JsonProperty(required = true)
    UUID messageId;
    @JsonProperty(required = true)
    String source;

    @JsonProperty(required = true)
    ActionType actionType;
    @JsonProperty(required = true)
    AktivitetskortType aktivitetskortType;
    @JsonProperty(required = true)
    Aktivitetskort aktivitetskort;
}
