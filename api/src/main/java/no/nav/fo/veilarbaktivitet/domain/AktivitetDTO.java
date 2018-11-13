package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class AktivitetDTO {

    public String id;
    public String versjon;

    public String tittel;
    public String beskrivelse;
    public String lenke;
    public AktivitetTypeDTO type;
    public AktivitetStatus status;
    public Date fraDato;
    public Date tilDato;
    public Date opprettetDato;
    public Date endretDato;
    public String endretAv;
    public boolean historisk;
    public String avsluttetKommentar;
    public boolean avtalt;
    public String lagtInnAv;
    public AktivitetTransaksjonsType transaksjonsType;
    public String malid;
    public Date lestAvBrukerForsteGang;
    public boolean automatiskOpprettet;

    // stillingaktivitet
    public EtikettTypeDTO etikett;
    public String kontaktperson;
    public String arbeidsgiver;
    public String arbeidssted;
    public String stillingsTittel;

    // egenaktivitet
    public String hensikt;
    public String oppfolging;

    //sokeAvtaleAktivitet
    public Long antallStillingerSokes;
    public String avtaleOppfolging;

    //iJobbAktivitet
    public JobbStatusTypeDTO jobbStatus;
    public String ansettelsesforhold;
    public String arbeidstid;

    //behandlingAktivitet
    public String behandlingType;
    public String behandlingSted;
    public String effekt;
    public String behandlingOppfolging;

    //møte
    public String adresse;
    public String forberedelser;
    public KanalDTO kanal;
    public String referat;
    public boolean erReferatPublisert;


}
