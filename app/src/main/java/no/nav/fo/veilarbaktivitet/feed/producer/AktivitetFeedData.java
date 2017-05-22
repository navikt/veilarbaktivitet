package no.nav.fo.veilarbaktivitet.feed.producer;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.fo.veilarbaktivitet.domain.AktivitetTypeData;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class AktivitetFeedData implements Comparable<AktivitetFeedData> {

    public String aktivitetId;
    public String aktorId;

    public Timestamp fraDato;
    public Timestamp tilDato;
    public Timestamp opprettetDato;

    AktivitetTypeData aktivitetType;
    AktivitetStatus status;
    public boolean avtalt;


    @Override
    public int compareTo(AktivitetFeedData o) {
        return opprettetDato.compareTo(o.opprettetDato);
    }
}
