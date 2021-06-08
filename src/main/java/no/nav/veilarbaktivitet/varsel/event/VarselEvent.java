package no.nav.veilarbaktivitet.varsel.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@SuperBuilder
public abstract class VarselEvent {

    @NonNull
    @Builder.Default
    UUID transactionId = UUID.randomUUID();

    @NonNull
    @Builder.Default
    LocalDateTime timestamp = LocalDateTime.now();

    @NonNull
    VarselEventType event;

}
