package no.nav.veilarbaktivitet.aktivitet.dto.filterTags;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Filters {
    private Filters() {}
    public static Optional<FilterTag> of(String categori, Object value) {
        if (value == null) return Optional.empty();
        if (value instanceof String stringValue) return Optional.of(new FilterTagString(categori, stringValue));
        if (value instanceof Boolean boolValue) return Optional.of(new FilterTagBool(categori, boolValue));
        return Optional.empty();
    }

    @SafeVarargs
    public static List<FilterTag> listOf(Optional<FilterTag> ...args) {
        return Arrays.stream(args)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }
}
