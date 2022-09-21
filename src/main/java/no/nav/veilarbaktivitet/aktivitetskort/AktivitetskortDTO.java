package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public class AktivitetskortDTO {
    UUID id;
    String utsender;
    LocalDate sendt;
    ActionType actionType;
    JsonNode payload;
}
