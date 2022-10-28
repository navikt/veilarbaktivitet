package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.With;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@With
public class Aktivitetskort {
    /*
    "id",
        "personIdent",
        "tittel",
        "aktivitetStatus",

        "endretAv",
        "endretTidspunkt",
        "avtaltMedNav"
     */
    @JsonProperty(required = true)
    UUID id;
    @JsonProperty(required = true)
    String personIdent;
    @JsonProperty(required = true)
    String tittel;
    String beskrivelse;
    @JsonProperty(required = true)
    AktivitetStatus aktivitetStatus;
    LocalDate startDato;
    LocalDate sluttDato;
    @JsonProperty(required = true)
    IdentDTO endretAv;
    LocalDateTime endretTidspunkt;
    @JsonProperty(required = true)
    Boolean avtaltMedNav;
    // String avsluttetBegrunnelse;

    OppgaveLenke oppgave;
    @Singular("handling")
    List<LenkeSeksjon> handlinger;
    @Singular("detalj")
    List<Attributt> detaljer;
    @Singular("etikett")
    List<Etikett> etiketter;
}
