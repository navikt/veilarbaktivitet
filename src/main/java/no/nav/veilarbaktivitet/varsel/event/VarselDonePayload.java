package no.nav.veilarbaktivitet.varsel.event;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Value
@SuperBuilder
public class VarselDonePayload extends VarselEvent {

    @NonNull
    @Builder.Default
    String system = "VEILARB_AKTIVITET";

    @NonNull
    String id;

    @NonNull
    String fodselsnummer;

    @NonNull
    String groupId;

}
