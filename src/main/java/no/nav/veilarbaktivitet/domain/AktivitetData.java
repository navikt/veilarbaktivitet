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

    /**
     * Teknisk id for aktiviteten
     */
    Long id;
    /**
     * Versjon inkrementeres når det utføres en transasjon på aktiviteten.
     * Denne er en global sekvens for alle aktiviteter, men for en enkelt aktivitet, vil en sortering på versjon gi rekkefølgen på transaksjonene
     */
    Long versjon;
    /**
     * AktørId for eksternbruker som 'eier' aktivitetsplanen
     */
    String aktorId;
    /**
     * Tittel på aktiviteten
     */
    String tittel;
    /**
     * Type aktivitet
     */
    AktivitetTypeData aktivitetType;
    String beskrivelse;
    /**
     * Overordnet status for aktiviteten i aktivitetsplanen
     */
    AktivitetStatus status;
    String avsluttetKommentar;
    /**
     * Gjelder for en AktivitetData versjon, og sier om det var NAV eller BRUKER som utførte en {@link AktivitetTransaksjonsType} på aktiviteten
     */
    InnsenderData lagtInnAv;
    Date fraDato;
    Date tilDato;
    String lenke;
    Date opprettetDato;
    Date endretDato;
    /**
     * Gjelder for en AktivitetData versjon, og angir ident som utførte en {@link AktivitetTransaksjonsType} på aktiviteten
     */
    String endretAv;
    /**
     * Merket som 'Avtalt med Nav'
     */
    boolean avtalt;
    Forhaandsorientering forhaandsorientering;
    /**
     * Alle type endringer på aktiviteten angis med en transaksjonstype.
     */
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

