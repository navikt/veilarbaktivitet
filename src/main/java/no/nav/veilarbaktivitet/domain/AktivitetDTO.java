package no.nav.veilarbaktivitet.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;

import java.util.Date;

@Data
@Accessors(chain = true)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AktivitetDTO {

    private String id;
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
    private String lagtInnAv;
    private AktivitetTransaksjonsType transaksjonsType;
    private String malid;

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
    private Long antallStillingerSokes;
    private Long antallStillingerIUken;
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
}
