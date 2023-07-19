package no.nav.veilarbaktivitet.aktivitetskort.dto.bestilling;

import lombok.Getter;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitetskort.ActionType;
import no.nav.veilarbaktivitet.aktivitetskort.Aktivitetskort;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortMapper;
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortType;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.person.Person;

import java.util.UUID;

@Getter
public class ArenaAktivitetskortBestilling extends AktivitetskortBestilling {
    private final ArenaId eksternReferanseId;
    private final String arenaTiltakskode;

    public ArenaAktivitetskortBestilling(Aktivitetskort aktivitetskort, String source, AktivitetskortType type, ArenaId eksternReferanseId, String arenaTiltakskode, UUID messageId, ActionType actionType, Person.AktorId aktorId) {
        super(source, type, aktivitetskort, messageId, actionType, aktorId);
        this.eksternReferanseId = eksternReferanseId;
        this.arenaTiltakskode = arenaTiltakskode;
    }

    @Override
    public AktivitetData toAktivitet() {
        var opprettetTidspunkt = this.getAktivitetskort().getEndretTidspunkt();
        return AktivitetskortMapper.mapTilAktivitetData(this, opprettetTidspunkt);
    }

    @Override
    public UUID getAktivitetskortId() {
        return this.getAktivitetskort().getId();
    }
}
