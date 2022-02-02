package no.nav.veilarbaktivitet.brukernotifikasjon;

public enum VarselType {
    STILLING_FRA_NAV, MOTE_SMS;

    public BrukernotifikasjonsType getBrukernotifikasjonType() {
        return switch (this) {
            case STILLING_FRA_NAV -> BrukernotifikasjonsType.OPPGAVE;
            case MOTE_SMS -> BrukernotifikasjonsType.BESKJED;
        };
    }
}
