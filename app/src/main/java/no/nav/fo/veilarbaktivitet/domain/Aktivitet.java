package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

import static java.util.Collections.emptyList;

@Data
@Accessors(chain = true)
public class Aktivitet {

    long id;
    String aktorId;
    String tittel;
    AktivitetType aktivitetType;
    String beskrivelse;
    AktivitetStatus status;
    Date avsluttetDato;
    String avsluttetKommentar;
    Innsender lagtInnAv;
    Date fraDato;
    Date tilDato;
    String lenke;
    boolean deleMedNav;
    Date opprettetDato;

    List<Kommentar> kommentarer = emptyList();

}

