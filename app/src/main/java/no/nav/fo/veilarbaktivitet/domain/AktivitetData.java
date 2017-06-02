package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class AktivitetData {

    Long id;
    long versjon;
    String aktorId;
    String tittel;
    AktivitetTypeData aktivitetType;
    String beskrivelse;
    AktivitetStatus status;
    String avsluttetKommentar;
    InnsenderData lagtInnAv;
    Date fraDato;
    Date tilDato;
    String lenke;
    Date opprettetDato;
    boolean avtalt;
    TransaksjonsTypeData transaksjonsTypeData;

    EgenAktivitetData egenAktivitetData;
    StillingsoekAktivitetData stillingsSoekAktivitetData;
    SokeAvtaleAktivitetData sokeAvtaleAktivitetData;

}

