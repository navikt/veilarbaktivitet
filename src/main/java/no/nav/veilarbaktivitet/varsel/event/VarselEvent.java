package no.nav.veilarbaktivitet.varsel.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class VarselEvent {

    @NonNull
    @Builder.Default
    UUID transactionId = UUID.randomUUID();

    @NonNull
    @Builder.Default
    LocalDateTime timestamp = LocalDateTime.now();

    @NonNull
    @Builder.Default
    String type = "VARSEL";

    @NonNull
    VarselEventType event;

    @NonNull
    VarselPayload payload;

}
