package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.Data;
import no.nav.tms.varsel.action.EksternVarslingBestilling;
import no.nav.tms.varsel.action.Varseltype;

@Data
public class VarselDto {
    public String varselId;
    public String ident;
    public String tekst;
    public String link;
    public Varseltype type;
    public EksternVarslingBestilling eksternVarsling;
    public long tidspunkt;
    public Produsent produsent;
}

@Data
class Produsent {
    String namespace;
    String appnavn;
    String cluster;
}
