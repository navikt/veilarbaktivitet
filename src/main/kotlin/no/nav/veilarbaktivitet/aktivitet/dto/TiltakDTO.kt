package no.nav.veilarbaktivitet.aktivitet.dto;

public record TiltakDTO (
    String tiltaksnavn,
    String arrangornavn,
    String deltakelseStatus,
    Integer dagerPerUke,
    Integer deltakelsesprosent
){}
