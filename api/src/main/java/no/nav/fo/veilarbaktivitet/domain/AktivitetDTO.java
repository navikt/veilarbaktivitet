package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
public class AktivitetDTO {

    public String id;
    public String tittel;
    public String beskrivelse;
    public String lenke;
    public AktivitetTypeDTO type;
    public AktivitetStatus status;
    public Date fraDato;
    public Date tilDato;
    public Date opprettetDato;
    public String avsluttetKommentar;

    public List<AktivitetTagDTO> tagger = new ArrayList<>();

    // stillingaktivitet
    public String etikett;
    public String kontaktperson;
    public String arbeidsgiver;
    public String arbeidssted;
    public String stillingsTittel;

    // egenaktivitet
    public String hensikt;

}
