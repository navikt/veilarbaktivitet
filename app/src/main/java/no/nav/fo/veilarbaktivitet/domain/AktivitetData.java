package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

import static java.util.Collections.emptyList;

@Data
@Accessors(chain = true)
public class AktivitetData {

    long id;
    String aktorId;
    String tittel;
    AktivitetTypeData aktivitetType;
    String beskrivelse;
    AktivitetStatusData status;
    Date avsluttetDato;
    String avsluttetKommentar;
    InnsenderData lagtInnAv;
    Date fraDato;
    Date tilDato;
    String lenke;
    Date opprettetDato;

    EgenAktivitetData egenAktivitetData;
    StillingsoekAktivitetData stillingsSoekAktivitetData;

}

