package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record TiltaksaktivitetData (
    String tiltakskode,
    String tiltaksnavn,
    String arrangornavn,
    String deltakelseStatus,
    Integer dagerPerUke,
    Integer deltakelsesprosent
) {}
