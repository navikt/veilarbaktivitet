package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.*;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;

import java.util.Date;
import java.util.UUID;

@Value
@With
@Builder(toBuilder = true)
@ToString(of = {"id", "funksjonellId", "versjon", "aktivitetType", "status", "endretDato", "transaksjonsType", "avtalt", "oppfolgingsperiodeId"})
public class AktivitetData {


    /**
     * Teknisk id for aktiviteten
     */
    public Long id;
    /**
     * Funksjonell id for aktiviteten
     */
    public UUID funksjonellId;
    /**
     * Versjon inkrementeres når det utføres en transasjon på aktiviteten.
     * Denne er en global sekvens for alle aktiviteter, men for en enkelt aktivitet, vil en sortering på versjon gi rekkefølgen på transaksjonene
     */
    public Long versjon;
    /**
     * AktørId for eksternbruker som 'eier' aktiviteten
     */
    public String aktorId;
    /**
     * Tittel på aktiviteten
     */
    public String tittel;
    /**
     * Type aktivitet
     */
    public AktivitetTypeData aktivitetType;
    public String beskrivelse;
    /**
     * Overordnet status for aktiviteten i aktivitetsplanen
     */
    public AktivitetStatus status;
    public String avsluttetKommentar;
    /**
     * Gjelder for en AktivitetData versjon, og sier om det var NAV eller BRUKER som utførte en {@link AktivitetTransaksjonsType} på aktiviteten
     */
    public Innsender endretAvType;
    public Date fraDato;
    public Date tilDato;
    public String lenke;
    public Date opprettetDato;
    public Date endretDato;
    /**
     * Gjelder for en AktivitetData versjon, og angir ident som utførte en {@link AktivitetTransaksjonsType} på aktiviteten
     */
    public String endretAv;
    /**
     * Merket som 'Avtalt med Nav'
     */
    public boolean avtalt;
    public Forhaandsorientering forhaandsorientering;
    /**
     * Alle type endringer på aktiviteten angis med en endringstype av type AktivitetTransaksjonsType.
     */
    public AktivitetTransaksjonsType transaksjonsType;
    public Date historiskDato;
    public String kontorsperreEnhetId;
    public Date lestAvBrukerForsteGang;
    public boolean automatiskOpprettet;
    public String malid;
    public String fhoId;

    public UUID oppfolgingsperiodeId;

    public EgenAktivitetData egenAktivitetData;
    public StillingsoekAktivitetData stillingsSoekAktivitetData;
    public SokeAvtaleAktivitetData sokeAvtaleAktivitetData;
    public IJobbAktivitetData iJobbAktivitetData;
    public BehandlingAktivitetData behandlingAktivitetData;
    public MoteData moteData;
    public StillingFraNavData stillingFraNavData;
    public EksternAktivitetData eksternAktivitetData;

    public boolean endringTillatt() {
        return !(AktivitetStatus.AVBRUTT.equals(status)
                || AktivitetStatus.FULLFORT.equals(status)
                || this.getHistoriskDato() != null);
    }
}

