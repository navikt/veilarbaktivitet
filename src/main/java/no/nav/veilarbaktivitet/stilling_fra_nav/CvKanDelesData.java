package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.With;
import no.nav.veilarbaktivitet.person.Innsender;

import java.util.Date;

@Builder(toBuilder = true)
@With
@Getter
@Data
public class CvKanDelesData {
    Boolean kanDeles;
    Date endretTidspunkt;
    String endretAv;
    Innsender endretAvType;
    Date avtaltDato;
}
