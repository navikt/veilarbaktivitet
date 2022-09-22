package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.Builder;

@Builder
public class TiltaksaktivitetData {
    String tiltakskode;
    String tiltaksnavn;
    String arrangornavn;
    String deltakelseStatus;
    Integer dagerPerUke;
    Integer deltakelsesprosent;
}
