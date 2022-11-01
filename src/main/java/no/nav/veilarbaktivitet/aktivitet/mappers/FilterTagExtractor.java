package no.nav.veilarbaktivitet.aktivitet.mappers;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.domain.StillingsoekAktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.filterTags.FilterTag;
import no.nav.veilarbaktivitet.aktivitet.dto.filterTags.Filters;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;

import java.util.List;
import java.util.Optional;

public class FilterTagExtractor {

    private FilterTagExtractor(){}

    public static Optional<FilterTag> getStilllingStatusFilter(AktivitetData aktivitet) {
        if (aktivitet.getAktivitetType() == AktivitetTypeData.JOBBSOEKING) {
            return Filters.of("stillingStatus", Optional.ofNullable(aktivitet.getStillingsSoekAktivitetData())
                    .map(StillingsoekAktivitetData::getStillingsoekEtikett)
                    .map(Enum::toString)
            );
        } else if (aktivitet.getAktivitetType() == AktivitetTypeData.STILLING_FRA_NAV) {
            return Filters.of("stillingStatus", Optional.ofNullable(aktivitet
                    .getStillingFraNavData())
                    .map(StillingFraNavData::getSoknadsstatus)
                    .map(Enum::toString)
            );
        }
        return Optional.empty();
    }

    public static List<FilterTag> getFilterTags(AktivitetData aktivitet) {
        return Filters.listOf(
            Filters.of("status", aktivitet.getStatus().toString()),
            Filters.of("aktivitetsType", aktivitet.getAktivitetType().toString()),
            Filters.of("avtaltAktivitet", aktivitet.isAvtalt()),
            getStilllingStatusFilter(aktivitet)
        );
    }

    public static List<FilterTag> getFilterTags(ArenaAktivitetDTO aktivitet) {
        return Filters.listOf(
            Filters.of("status", aktivitet.getStatus().toString()),
            Filters.of("aktivitetsType", aktivitet.getType().toString()),
            Filters.of("avtaltAktivitet", aktivitet.isAvtalt()),
            Filters.of("tiltakstatus", aktivitet.getEtikett().toString())
        );
    }
}
