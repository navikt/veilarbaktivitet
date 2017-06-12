package no.nav.fo.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
public class AktivitetData {

    Long id;
    Long versjon;
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

