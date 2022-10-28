package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;
@Builder
public class KafkaAktivitetskortWrapperDTO {
    @JsonProperty(required = true)
    UUID messageId;
    @JsonProperty(required = true)
    String source;
    @JsonProperty(required = true)
    LocalDateTime sendt;

    @JsonProperty(required = true)
    ActionType actionType;
    @JsonProperty(required = true)
    AktivitetskortType aktivitetskortType;
    @JsonProperty(required = true)
    Aktivitetskort aktivitetskort;
}
