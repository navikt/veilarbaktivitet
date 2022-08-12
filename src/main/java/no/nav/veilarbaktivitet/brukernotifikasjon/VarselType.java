package no.nav.veilarbaktivitet.brukernotifikasjon;

public enum VarselType {
    STILLING_FRA_NAV, MOTE_SMS, FORHAANDSORENTERING, CV_DELT;

    public BrukernotifikasjonsType getBrukernotifikasjonType() {
        return switch (this) {
            case STILLING_FRA_NAV, FORHAANDSORENTERING -> BrukernotifikasjonsType.OPPGAVE;
            case MOTE_SMS, CV_DELT -> BrukernotifikasjonsType.BESKJED;
        };
    }
}
