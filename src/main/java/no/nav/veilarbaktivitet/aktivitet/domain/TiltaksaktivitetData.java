package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.Builder;

@Builder
public class TiltaksaktivitetData {
    public String tiltakskode;
    public String tiltaksnavn;
    public String arrangornavn;
    public String deltakelseStatus;
    public Integer dagerPerUke;
    public Integer deltakelsesprosent;
}
