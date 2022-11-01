package no.nav.veilarbaktivitet.aktivitet.mappers;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.dto.filterTags.FilterTag;
import no.nav.veilarbaktivitet.aktivitet.dto.filterTags.Filters;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;

import java.util.List;
import java.util.Objects;

public class FilterMapper {
    public static FilterTag getStilllingStatusFilter(AktivitetData aktivitet) {
        if (aktivitet.getAktivitetType() == AktivitetTypeData.JOBBSOEKING) {
            return Filters.of("stillingStatus", aktivitet
                    .getStillingsSoekAktivitetData()
                    .getStillingsoekEtikett().toString()
            );
        } else if (aktivitet.getAktivitetType() == AktivitetTypeData.STILLING_FRA_NAV) {
            return Filters.of("stillingStatus", aktivitet
                    .getStillingFraNavData()
                    .getSoknadsstatus()
                    .toString()
            );
        }
        return null;
    }

    public static List<FilterTag> getFilterTags(AktivitetData aktivitet) {
        return List.of(
                Filters.of("status", aktivitet.getStatus().toString()),
                Filters.of("aktivitetsType", aktivitet.getAktivitetType().toString()),
                Filters.of("avtaltAktivitet", aktivitet.isAvtalt()),
                getStilllingStatusFilter(aktivitet)
            )
            .stream()
            .filter(Objects::nonNull)
            .toList();
    }

    public static List<FilterTag> getFilterTags(ArenaAktivitetDTO aktivitet) {
        return List.of(
                Filters.of("status", aktivitet.getStatus().toString()),
                Filters.of("aktivitetsType", aktivitet.getType().toString()),
                Filters.of("avtaltAktivitet", aktivitet.isAvtalt()),
                Filters.of("tiltakstatus", aktivitet.getEtikett().toString())
            )
            .stream()
            .filter(Objects::nonNull)
            .toList();
    }
}
