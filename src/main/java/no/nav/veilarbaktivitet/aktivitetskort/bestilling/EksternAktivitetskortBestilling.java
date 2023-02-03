package no.nav.veilarbaktivitet.aktivitetskort.bestilling;

import lombok.Getter;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitetskort.ActionType;
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;
import no.nav.veilarbaktivitet.person.Person;

import java.util.UUID;

@Getter
public class EksternAktivitetskortBestilling extends AktivitetskortBestilling {

    public EksternAktivitetskortBestilling(Aktivitetskort aktivitetskort, String source, AktivitetskortType type, UUID messageId, ActionType actionType, Person.AktorId aktorId) {
        super(source, type, aktivitetskort, messageId, actionType, aktorId);
    }

    @Override
    public AktivitetData toAktivitet() {
        return AktivitetskortMapper.mapTilAktivitetData(this, null);
    }

    @Override
    public UUID getAktivitetskortId() {
        return this.getAktivitetskort().getId();
    }
}