package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
class VarselEntity {
    final String varselId;
    final long aktivitetId;
    final Varseltype type;
    final VarselStatus status;
    final LocalDateTime sendt;
    final LocalDateTime varselFeilet;
    final LocalDateTime avsluttet;
}
