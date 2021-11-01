package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.Builder;
import lombok.Value;
import no.nav.veilarbaktivitet.brukernotifikasjon.kvitering.VarselKvitteringStatus;

import java.util.Date;

@Value
@Builder
public class Brukernotifikasjon {
    long id;
    String brukernotifikasjonId;
    long aktivitetId;
    long opprettetPaaAktivitetVersjon;
    String foedselsnummer;
    String oppfolgingsperiode;
    VarselType type;
    VarselStatus status;
    VarselKvitteringStatus varselKvitteringStatus;
    Date opprettet;
    String melding;
    Date varselFeilet;
    Date avsluttet;
    Date bekreftetSendt;
    Date forsoktSendt;
    Date ferdigBehandlet;
}
