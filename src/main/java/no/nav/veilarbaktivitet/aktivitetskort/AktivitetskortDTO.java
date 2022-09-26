package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public class AktivitetskortDTO {
    UUID messageId;
    String source;
    LocalDateTime sendt;
    ActionType actionType;
    JsonNode payload;
}
