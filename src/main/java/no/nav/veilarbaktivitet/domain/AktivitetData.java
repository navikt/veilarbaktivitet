package no.nav.veilarbaktivitet.domain;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.With;
import no.nav.veilarbaktivitet.avtaltMedNav.Forhaandsorientering;

import java.util.Date;

@Value
@With
@Builder(toBuilder = true)
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
    Forhaandsorientering forhaandsorientering;
    AktivitetTransaksjonsType transaksjonsType;
    Date historiskDato;
    String kontorsperreEnhetId;
    Date lestAvBrukerForsteGang;
    boolean automatiskOpprettet;
    String malid;
    String fhoId;

    EgenAktivitetData egenAktivitetData;
    StillingsoekAktivitetData stillingsSoekAktivitetData;
    SokeAvtaleAktivitetData sokeAvtaleAktivitetData;
    IJobbAktivitetData iJobbAktivitetData;
    BehandlingAktivitetData behandlingAktivitetData;
    MoteData moteData;
    StillingFraNavData stillingFraNavData;
}

