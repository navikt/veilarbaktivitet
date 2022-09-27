package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.With;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.person.InnsenderData;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;

import java.util.Date;
import java.util.UUID;

@Value
@With
@Builder(toBuilder = true)
@ToString(of = {"id", "funksjonellId", "versjon", "aktivitetType", "status", "endretDato", "transaksjonsType", "avtalt", "oppfolgingsperiodeId", "tiltaksaktivitetData"})
public class AktivitetData {

    /**
     * Teknisk id for aktiviteten
     */
    Long id;
    /**
     * Funksjonell id for aktiviteten
     */
    UUID funksjonellId;
    /**
     * Versjon inkrementeres når det utføres en transasjon på aktiviteten.
     * Denne er en global sekvens for alle aktiviteter, men for en enkelt aktivitet, vil en sortering på versjon gi rekkefølgen på transaksjonene
     */
    Long versjon;
    /**
     * AktørId for eksternbruker som 'eier' aktiviteten
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
     * Alle type endringer på aktiviteten angis med en endringstype av type AktivitetTransaksjonsType.
     */
    AktivitetTransaksjonsType transaksjonsType;
    Date historiskDato;
    String kontorsperreEnhetId;
    Date lestAvBrukerForsteGang;
    boolean automatiskOpprettet;
    String malid;
    String fhoId;

    UUID oppfolgingsperiodeId;

    EgenAktivitetData egenAktivitetData;
    StillingsoekAktivitetData stillingsSoekAktivitetData;
    SokeAvtaleAktivitetData sokeAvtaleAktivitetData;
    IJobbAktivitetData iJobbAktivitetData;
    BehandlingAktivitetData behandlingAktivitetData;
    MoteData moteData;
    StillingFraNavData stillingFraNavData;
    TiltaksaktivitetData tiltaksaktivitetData;
}

