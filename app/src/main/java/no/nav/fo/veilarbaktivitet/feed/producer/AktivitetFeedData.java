package no.nav.fo.veilarbaktivitet.feed.producer;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;

import java.util.Date;

@Data
@Accessors(chain = true)
public class AktivitetFeedData implements Comparable<AktivitetFeedData> {

    String aktivitetId;
    String aktorId;

    Date fraDato;
    Date tilDato;
    Date opprettetDato;

    AktivitetTypeData aktivitetType;
    AktivitetStatus status;
    boolean avtalt;

    @Override
    public int compareTo(AktivitetFeedData o) {
        return opprettetDato.compareTo(o.opprettetDato);
    }
}
