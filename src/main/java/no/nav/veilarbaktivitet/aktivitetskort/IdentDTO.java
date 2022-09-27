package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record IdentDTO (
    String ident,
    IdentType identType)
{
}
