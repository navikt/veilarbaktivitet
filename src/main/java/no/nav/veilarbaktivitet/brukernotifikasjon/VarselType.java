package no.nav.veilarbaktivitet.brukernotifikasjon;

import java.util.Arrays;
import java.util.List;

import static no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonsType.BESKJED;
import static no.nav.veilarbaktivitet.brukernotifikasjon.BrukernotifikasjonsType.OPPGAVE;

public enum VarselType {
    STILLING_FRA_NAV(OPPGAVE),
    FORHAANDSORENTERING(OPPGAVE),
    MOTE_SMS(BESKJED),
    CV_DELT(BESKJED),
    IKKE_FATT_JOBBEN(BESKJED);

    private final BrukernotifikasjonsType brukernotifikasjonsType;

    VarselType(BrukernotifikasjonsType beskjed) {
        this.brukernotifikasjonsType = beskjed;
    }

    public BrukernotifikasjonsType getBrukernotifikasjonType() {
        return brukernotifikasjonsType;
    }

    public List<VarselType> varslerForBrukernotifikasjonstype(BrukernotifikasjonsType wanted) {
        return Arrays.stream(values()).filter(v -> v.brukernotifikasjonsType == wanted).toList();
    }

}
