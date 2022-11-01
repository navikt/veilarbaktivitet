package no.nav.veilarbaktivitet.aktivitet.mappers;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData;
import no.nav.veilarbaktivitet.aktivitet.dto.filterTags.FilterTag;
import no.nav.veilarbaktivitet.aktivitet.dto.filterTags.Filters;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.stilling_fra_nav.StillingFraNavData;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class FilterMapper {
    public static Optional<FilterTag> getStilllingStatusFilter(AktivitetData aktivitet) {
        if (aktivitet.getAktivitetType() == AktivitetTypeData.JOBBSOEKING) {
            return Filters.of("stillingStatus", aktivitet
                    .getStillingsSoekAktivitetData()
                    .getStillingsoekEtikett().toString()
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
        try {
            return Stream.of(
                            Filters.of("status", aktivitet.getStatus().toString()),
                            Filters.of("aktivitetsType", aktivitet.getAktivitetType().toString()),
                            Filters.of("avtaltAktivitet", aktivitet.isAvtalt()),
                            getStilllingStatusFilter(aktivitet)
                    )
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        } catch (Exception e) {
            System.out.println(e);
            return List.of();
        }

    }

    public static List<FilterTag> getFilterTags(ArenaAktivitetDTO aktivitet) {
        try {
            return Stream.of(
                            Filters.of("status", aktivitet.getStatus().toString()),
                            Filters.of("aktivitetsType", aktivitet.getType().toString()),
                            Filters.of("avtaltAktivitet", aktivitet.isAvtalt()),
                            Filters.of("tiltakstatus", aktivitet.getEtikett().toString())
                    )
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        } catch (Exception e) {
            System.out.println(e);
            return List.of();
        }
    }
}
