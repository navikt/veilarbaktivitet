package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.With;

@Builder(toBuilder = true)
@With
@Getter
@Data
public class KontaktpersonData {
    String navn;
    String tittel;
    String mobil;
    String epost;
}
