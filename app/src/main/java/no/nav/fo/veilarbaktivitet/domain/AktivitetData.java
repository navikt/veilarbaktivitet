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
    Date endretDato;
    String endretAv;
    boolean avtalt;
    AktivitetTransaksjonsType transaksjonsType;
    Date historiskDato;

    EgenAktivitetData egenAktivitetData;
    StillingsoekAktivitetData stillingsSoekAktivitetData;
    SokeAvtaleAktivitetData sokeAvtaleAktivitetData;
    IJobbAktivitetData iJobbAktivitetData;
    BehandlingAktivitetData behandlingAktivitetData;

}

