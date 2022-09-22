package no.nav.veilarbaktivitet.aktivitet.domain;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

@Builder
public class TiltaksaktivitetData {
    String tiltakskode;
    String tiltaksnavn;
    String aarsak;
    String arrangornavn;
    Integer deltakelsesprosent;
    Integer dagerPerUke;
    LocalDateTime registrertDato;
    LocalDateTime statusEndretDato;
}
