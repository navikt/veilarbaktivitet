package no.nav.veilarbaktivitet.avtalt_med_nav;

import lombok.Builder;
import lombok.Data;

import java.util.Date;


@Data
@Builder(toBuilder = true)
public class ForhaandsorienteringDTO {
    private String id;
    private Type type;
    private String tekst;
    private Date lestDato;

}
