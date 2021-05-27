package no.nav.veilarbaktivitet.varsel.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class CreateVarselPayload implements VarselPayload {

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
