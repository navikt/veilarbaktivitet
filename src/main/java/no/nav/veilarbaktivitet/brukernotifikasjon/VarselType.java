package no.nav.veilarbaktivitet.brukernotifikasjon;

public enum VarselType {
    STILLING_FRA_NAV, MOTE_SMS, FORHAANDSORENTERING;

    public BrukernotifikasjonsType getBrukernotifikasjonType() {
        return switch (this) {
            case STILLING_FRA_NAV, FORHAANDSORENTERING -> BrukernotifikasjonsType.OPPGAVE;
            case MOTE_SMS -> BrukernotifikasjonsType.BESKJED;
        };
    }
}
