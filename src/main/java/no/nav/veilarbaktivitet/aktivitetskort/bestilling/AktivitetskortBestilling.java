package no.nav.veilarbaktivitet.aktivitetskort.bestilling;

import lombok.Getter;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitetskort.ActionType;
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;
import no.nav.veilarbaktivitet.aktivitetskort.dto.BestillingBase;
import no.nav.veilarbaktivitet.person.Person;

import java.util.UUID;

@Getter
public abstract class AktivitetskortBestilling extends BestillingBase {
    public final AktivitetskortType aktivitetskortType;
    public final Aktivitetskort aktivitetskort;
    public final Person.AktorId aktorId;

    protected AktivitetskortBestilling(String source, AktivitetskortType aktivitetskortType, Aktivitetskort aktivitetskort, UUID messageId, ActionType actionType, Person.AktorId aktorId) {
        super(source, messageId, actionType);
        this.aktivitetskortType = aktivitetskortType;
        this.aktivitetskort = aktivitetskort;
        this.aktorId = aktorId;
    }

    public abstract AktivitetData toAktivitet();
}
