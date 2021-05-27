package no.nav.veilarbaktivitet.varsel.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;

@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
public class VarselDonePayload implements VarselPayload {

    @NonNull
    @Builder.Default
    private String system = "VEILARB_AKTIVITET";

    @NonNull
    private String id;

    @NonNull
    private String fodselsnummer;

    @NonNull
    private String groupId;

}
