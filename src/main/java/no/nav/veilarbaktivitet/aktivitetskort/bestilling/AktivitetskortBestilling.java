package no.nav.veilarbaktivitet.aktivitetskort.bestilling;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitetskort.ActionType;
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;
import no.nav.veilarbaktivitet.person.Person;

import java.util.UUID;

@Getter
public abstract class AktivitetskortBestilling {
    private final String source;
    private final AktivitetskortType aktivitetskortType;
    private final Aktivitetskort aktivitetskort;
    private final UUID messageId;
    private final ActionType actionType;
    private final Person.AktorId aktorId;

    protected AktivitetskortBestilling(String source, AktivitetskortType aktivitetskortType, Aktivitetskort aktivitetskort, UUID messageId, ActionType actionType, Person.AktorId aktorId) {
        this.source = source;
        this.aktivitetskortType = aktivitetskortType;
        this.aktivitetskort = aktivitetskort;
        this.messageId = messageId;
        this.actionType = actionType;
        this.aktorId = aktorId;
    }

    public abstract AktivitetData toAktivitet();
}
