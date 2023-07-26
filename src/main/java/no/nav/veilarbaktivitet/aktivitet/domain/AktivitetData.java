package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import no.nav.veilarbaktivitet.avtalt_med_nav.Forhaandsorientering;
import no.nav.veilarbaktivitet.person.Innsender;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;

import java.util.Date;
import java.util.UUID;

@Getter
@With
@Builder(toBuilder = true)
@ToString(of = {"id", "funksjonellId", "versjon", "aktivitetType", "status", "endretDato", "transaksjonsType", "avtalt", "oppfolgingsperiodeId"})
public class AktivitetData {


    /**
     * Teknisk id for aktiviteten
     */
    private Long id;
    /**
     * Funksjonell id for aktiviteten
     */
    private UUID funksjonellId;
    /**
     * Versjon inkrementeres når det utføres en transasjon på aktiviteten.
     * Denne er en global sekvens for alle aktiviteter, men for en enkelt aktivitet, vil en sortering på versjon gi rekkefølgen på transaksjonene
     */
    private Long versjon;
    /**
     * AktørId for eksternbruker som 'eier' aktiviteten
     */
    private String aktorId;
    /**
     * Tittel på aktiviteten
     */
    private String tittel;
    /**
     * Type aktivitet
     */
    private AktivitetTypeData aktivitetType;
    private String beskrivelse;
    /**
     * Overordnet status for aktiviteten i aktivitetsplanen
     */
    private AktivitetStatus status;
    private String avsluttetKommentar;
    /**
     * Gjelder for en AktivitetData versjon, og sier om det var NAV eller BRUKER som utførte en {@link AktivitetTransaksjonsType} på aktiviteten
     */
    private Innsender endretAvType;
    private Date fraDato;
    private Date tilDato;
    private String lenke;
    private Date opprettetDato;
    private Date endretDato;
    /**
     * Gjelder for en AktivitetData versjon, og angir ident som utførte en {@link AktivitetTransaksjonsType} på aktiviteten
     */
    private String endretAv;
    /**
     * Merket som 'Avtalt med Nav'
     */
    private boolean avtalt;
    private Forhaandsorientering forhaandsorientering;
    /**
     * Alle type endringer på aktiviteten angis med en endringstype av type AktivitetTransaksjonsType.
     */
    private AktivitetTransaksjonsType transaksjonsType;
    private Date historiskDato;
    private String kontorsperreEnhetId;
    private Date lestAvBrukerForsteGang;
    private boolean automatiskOpprettet;
    private String malid;
    private String fhoId;

    private UUID oppfolgingsperiodeId;

    private EgenAktivitetData egenAktivitetData;
    private StillingsoekAktivitetData stillingsSoekAktivitetData;
    private SokeAvtaleAktivitetData sokeAvtaleAktivitetData;
    private IJobbAktivitetData iJobbAktivitetData;
    private BehandlingAktivitetData behandlingAktivitetData;
    private MoteData moteData;
    private StillingFraNavData stillingFraNavData;
    private EksternAktivitetData eksternAktivitetData;

    public boolean endringTillatt() {
        return !(AktivitetStatus.AVBRUTT.equals(status)
                || AktivitetStatus.FULLFORT.equals(status)
                || this.getHistoriskDato() != null);
    }
}

