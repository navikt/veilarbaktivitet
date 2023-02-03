package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.With;
import no.nav.veilarbaktivitet.aktivitet.domain.Ident;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.LenkeSeksjon;
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Oppgaver;
import no.nav.veilarbaktivitet.aktivitetskort.util.ZonedOrNorwegianDateTimeDeserializer;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@With
public class Aktivitetskort {

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
    Ident endretAv;

    @JsonProperty(required = true)
    @JsonDeserialize(using = ZonedOrNorwegianDateTimeDeserializer.class)
    ZonedDateTime endretTidspunkt;

    @JsonProperty(required = true)
    boolean avtaltMedNav;

    Oppgaver oppgave;

    @Singular("handling")
    List<LenkeSeksjon> handlinger;

    @Singular("detalj")
    List<Attributt> detaljer;

    @Singular("etikett")
    List<Etikett> etiketter;
}
