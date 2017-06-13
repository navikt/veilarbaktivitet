package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class AktivitetFeedData implements Comparable<AktivitetFeedData> {

    String aktivitetId;
    String aktorId;

    Date fraDato;
    Date tilDato;
    Date endretDato;

    AktivitetTypeData aktivitetType;
    AktivitetStatus status;
    boolean avtalt;

    @Override
    public int compareTo(AktivitetFeedData o) {
        return endretDato.compareTo(o.endretDato);
    }
}
