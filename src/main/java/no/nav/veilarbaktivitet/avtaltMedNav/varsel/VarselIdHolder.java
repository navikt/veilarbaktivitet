package no.nav.veilarbaktivitet.avtaltMedNav.varsel;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VarselIdHolder {
    private String id;
    private String arenaaktivitetId;
    private String aktivitetId;
    private String aktorId;

    public void validate() {
        if (id == null) {
            throw new IllegalStateException("Varsel må alltid ha en id");
        }

        if (aktorId == null) {
            throw new IllegalStateException("AktorId må alltid være satt");
        }

        if (arenaaktivitetId != null && aktivitetId != null) {
            throw new IllegalStateException("Varsel har både arena- og aktivitetsid, dette skal ikke være mulig");
        }

        if (arenaaktivitetId == null && aktivitetId == null) {
            throw new IllegalStateException("Varsel må enten ha en arenaaktivitetsid eller en aktivitetid");
        }
    }
}
