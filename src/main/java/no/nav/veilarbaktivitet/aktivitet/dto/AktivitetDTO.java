package no.nav.veilarbaktivitet.aktivitet.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;

import java.util.Date;
import java.util.UUID;

@Data
@Accessors(chain = true)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AktivitetDTO {

    private String id;
    private UUID funksjonellId;
    private String versjon;

    private String tittel;
    private String beskrivelse;
    private String lenke;
    private AktivitetTypeDTO type;
    private AktivitetStatus status;
    private Date fraDato;
    private Date tilDato;
    private Date opprettetDato;
    private Date endretDato;
    private String endretAv;
    private boolean historisk;
    private String avsluttetKommentar;
    private boolean avtalt;
    private ForhaandsorienteringDTO forhaandsorientering;
    private String endretAvType;

    @JsonGetter("lagtInnAv")
    public String getLagtInnAv() {
        return endretAvType;
    }

    @JsonSetter("lagtInnAv")
    private void setLagtInnAv(String todo) {
        // TODO: 19/01/2023 Denne bør vere midlertidig til vi har migrert aktiviteslan og dialog
        //dete trengs for ikke ødlege serialiseringen vår.
        //bør ikke ødlegge noe da vi ikke leser denne atributten (eller endret av) untat ved json serialisering.
        //og vi ikke gjenbruker aktivitetDTOene
        //bør merges når vi er klare til i oppdatere arbeidsrettet-dialog og aktivitesplan
        endretAvType = todo;
    }

    private AktivitetTransaksjonsType transaksjonsType;
    private String malid;
    private UUID oppfolgingsperiodeId;

    // stillingaktivitet
    private EtikettTypeDTO etikett;
    private String kontaktperson;
    private String arbeidsgiver;
    private String arbeidssted;
    private String stillingsTittel;

    // egenaktivitet
    private String hensikt;
    private String oppfolging;

    //sokeAvtaleAktivitet
    private Integer antallStillingerSokes;
    private Integer antallStillingerIUken;
    private String avtaleOppfolging;

    //iJobbAktivitet
    private JobbStatusTypeDTO jobbStatus;
    private String ansettelsesforhold;
    private String arbeidstid;

    //behandlingAktivitet
    private String behandlingType;
    private String behandlingSted;
    private String effekt;
    private String behandlingOppfolging;

    //møte
    private String adresse;
    private String forberedelser;
    private KanalDTO kanal;
    private String referat;
    private boolean erReferatPublisert;

    private StillingFraNavData stillingFraNavData;

    private EksternAktivitetDTO eksternAktivitet;
}
