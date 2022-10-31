package no.nav.veilarbaktivitet.aktivitetskort.bestilling;

import lombok.Getter;
import no.nav.veilarbaktivitet.aktivitetskort.ActionType;
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;
import no.nav.veilarbaktivitet.arena.model.ArenaId;

import java.util.UUID;

@Getter
public class ArenaAktivitetskortBestilling extends AktivitetskortBestilling {
    private final ArenaId eksternReferanseId;
    private final String arenaTiltakskode;

    public ArenaAktivitetskortBestilling(Aktivitetskort aktivitetskort, String source, AktivitetskortType type, ArenaId eksternReferanseId, String arenaTiltakskode, UUID messageId, ActionType actionType) {
        super(source, type, aktivitetskort, messageId, actionType, aktorId, oppfolgingsPeriode);
        this.eksternReferanseId = eksternReferanseId;
        this.arenaTiltakskode = arenaTiltakskode;
    }
}
