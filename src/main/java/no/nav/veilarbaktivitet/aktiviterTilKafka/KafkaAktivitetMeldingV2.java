package no.nav.veilarbaktivitet.aktiviterTilKafka;

import lombok.Builder;
import lombok.Value;
import no.nav.veilarbaktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.domain.AktivitetTypeDTO;

import java.util.Date;

@Value
@Builder
public class KafkaAktivitetMeldingV2 {
    String aktivitetId;
    String version;
    String aktorId;
    Date fraDato;
    Date tilDato;
    Date endretDato;
    AktivitetTypeDTO aktivitetType;
    AktivitetStatus aktivitetStatus;
    boolean avtalt;
    boolean historisk;

}
