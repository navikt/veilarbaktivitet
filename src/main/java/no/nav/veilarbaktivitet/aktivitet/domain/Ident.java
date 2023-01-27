package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.Builder;
import lombok.With;
import no.nav.veilarbaktivitet.aktivitetskort.dto.IdentType;
import no.nav.veilarbaktivitet.person.Innsender;

@Builder
@With
public record Ident(
    String ident,
    IdentType identType)
{
    public Ident(String ident, Innsender innsender) {
        this(ident, innsender.toIdentType());
    }
}
