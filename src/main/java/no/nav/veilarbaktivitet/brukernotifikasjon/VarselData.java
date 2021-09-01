package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.Data;

import java.time.LocalDateTime;

@Data
class VarselEntity {
    private final long id;
    private final String varsel_id;
    private final long aktivitet_id;
    private final Varseltype type;
    private final VarselStatus status;
    private final LocalDateTime sendt;
    private final LocalDateTime varsel_feilet;
    private final LocalDateTime avsluttet;
}
