package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.Builder;

import java.util.Date;

@Builder
public class TiltaksaktivitetData {
    String tiltakskode;
    String tiltaksnavn;
    String aarsak;
    String arrangornavn;
    Integer deltakelsesprosent;
    Integer dagerPerUke;
    Date registrertDato;
    Date statusEndretDato;
}
