package no.nav.veilarbaktivitet.veilarbportefolje.dto;

import lombok.Builder;
import lombok.Value;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.person.InnsenderData;
import no.nav.veilarbaktivitet.veilarbportefolje.EndringsType;
import no.nav.veilarbaktivitet.veilarbportefolje.StillingFraNavPortefoljeData;

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
    String tiltakskode;
}
