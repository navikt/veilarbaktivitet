package no.nav.veilarbaktivitet.aktivitetskort.bestilling;

import no.nav.veilarbaktivitet.aktivitetskort.ActionType;
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;

import java.util.UUID;

public class EksternAktivitetskortBestilling extends AktivitetskortBestilling {

    public EksternAktivitetskortBestilling(Aktivitetskort aktivitetskort, String source, AktivitetskortType type, UUID messageId, ActionType actionType) {
        super(source, type, aktivitetskort, messageId, actionType, aktorId, oppfolgingsPeriode);
    }

}