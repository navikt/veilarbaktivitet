package no.nav.veilarbaktivitet.avtaltMedNav;

import lombok.Builder;
import lombok.Data;
import org.joda.time.DateTime;


@Data
@Builder(toBuilder = true)
public class ForhaandsorienteringDTO {
    private String id;
    private Type type;
    private String tekst;
    private DateTime lestDato;

}
