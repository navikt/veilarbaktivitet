package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@ToString(of = {"payload"})
public class KafkaAktivitetWrapperDTO {
    UUID messageId;
    String source;
    LocalDateTime sendt;
    ActionType actionType;
    JsonNode payload;
}
