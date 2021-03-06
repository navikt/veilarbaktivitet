package no.nav.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class AktivitetFeedData implements Comparable<AktivitetFeedData> {

    public static final String FEED_NAME = "aktiviteter";

    String aktivitetId;
    String aktorId;

    Date fraDato;
    Date tilDato;
    Date endretDato;

    AktivitetTypeDTO aktivitetType;
    AktivitetStatus status;
    boolean avtalt;
    boolean historisk;

    @Override
    public int compareTo(AktivitetFeedData o) {
        return endretDato.compareTo(o.endretDato);
    }

}
