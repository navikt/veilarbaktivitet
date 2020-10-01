package no.nav.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Wither;

import java.time.ZonedDateTime;

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
    ZonedDateTime fraDato;
    ZonedDateTime tilDato;
    String lenke;
    ZonedDateTime opprettetDato;
    ZonedDateTime endretDato;
    String endretAv;
    boolean avtalt;
    AktivitetTransaksjonsType transaksjonsType;
    ZonedDateTime historiskDato;
    String kontorsperreEnhetId;
    ZonedDateTime lestAvBrukerForsteGang;
    boolean automatiskOpprettet;
    String malid;

    EgenAktivitetData egenAktivitetData;
    StillingsoekAktivitetData stillingsSoekAktivitetData;
    SokeAvtaleAktivitetData sokeAvtaleAktivitetData;
    IJobbAktivitetData iJobbAktivitetData;
    BehandlingAktivitetData behandlingAktivitetData;
    MoteData moteData;

}

