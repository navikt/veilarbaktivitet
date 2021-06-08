package no.nav.veilarbaktivitet.varsel.event;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Value
@SuperBuilder
public class CreateVarselPayload extends VarselEvent {

    @NonNull
    @Builder.Default
    String system = "VEILARB_AKTIVITET";

    @NonNull
    String id;

    @NonNull
    VarselType type;

    @NonNull
    String fodselsnummer;

    @NonNull
    String groupId;

    @NonNull
    String message;

    @NonNull
    String link;

    @NonNull
    @Builder.Default
    int sikkerhetsnivaa = 4;

    @Builder.Default
    LocalDateTime visibleUntil = null;

    @NonNull
    @Builder.Default
    boolean externalVarsling = false;
}
