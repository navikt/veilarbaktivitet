package no.nav.veilarbaktivitet.veilarbportefolje;

import lombok.Builder;
import lombok.Value;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO;
import no.nav.veilarbaktivitet.person.InnsenderData;

import java.util.Date;

@Value
@Builder
public class KafkaAktivitetMeldingV4 {
    String aktivitetId;
    Long version;
    String aktorId;
    Date fraDato;
    Date tilDato;
    Date endretDato;
    AktivitetTypeDTO aktivitetType;
    AktivitetStatus aktivitetStatus;
    EndringsType endringsType;
    InnsenderData lagtInnAv;
    StillingFraNavPortefoljeData stillingFraNavData;
    boolean avtalt;
    boolean historisk;
}
