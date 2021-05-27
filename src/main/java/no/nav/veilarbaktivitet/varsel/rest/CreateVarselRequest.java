package no.nav.veilarbaktivitet.varsel.rest;

import lombok.Value;
import no.nav.veilarbaktivitet.varsel.event.VarselType;

import java.time.LocalDateTime;

@Value
public class CreateVarselRequest {
    VarselType type;
    String fodselsnummer;
    String groupId;
    String message;
    String link;
    LocalDateTime visibleUntil;
    boolean externalVarsling;

}
