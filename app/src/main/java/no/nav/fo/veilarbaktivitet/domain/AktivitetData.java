package no.nav.fo.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Wither;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@Wither
@ToString(of = {"id", "versjon", "aktivitetType", "status", "endretDato", "transaksjonsType", "avtalt"})
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
    String kontorsperreEnhetId;

    EgenAktivitetData egenAktivitetData;
    StillingsoekAktivitetData stillingsSoekAktivitetData;
    SokeAvtaleAktivitetData sokeAvtaleAktivitetData;
    IJobbAktivitetData iJobbAktivitetData;
    BehandlingAktivitetData behandlingAktivitetData;
    MoteData moteData;

}

