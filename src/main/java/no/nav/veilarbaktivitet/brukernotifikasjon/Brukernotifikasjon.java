package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class Brukernotifikasjon {
    private long id;
    private String brukernotifikasjonId;
    private long aktivitetId;
    private long opprettetPaaAktivitetVersjon;
    private String foedselsnummer;
    private String oppfolgingsperiode;
    private VarselType type;
    private VarselStatus status;
    private VarselFunksjon funksjon;
    private Date opprettet;
    private String melding;
    private Date varselFeilet;
    private Date avsluttet;
    private Date bekreftetSendt;
    private Date forsoktSendt;
    private Date ferdigBehandlet;
}
