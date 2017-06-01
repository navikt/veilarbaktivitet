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
    public String avsluttetKommentar;
    public boolean avtalt;

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
    public Long antall;
    public String avtaleOppfolging;
}
