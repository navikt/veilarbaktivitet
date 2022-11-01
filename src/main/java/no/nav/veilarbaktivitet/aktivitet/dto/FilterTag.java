package no.nav.veilarbaktivitet.aktivitet.dto;

import lombok.RequiredArgsConstructor;

public interface FilterTag {}

@RequiredArgsConstructor
public class FilterTagString implements FilterTag {
    public final String kategori;
    public final String verdi;
}

@RequiredArgsConstructor
public class FilterTagBool implements FilterTag {
    public final String kategori;
    public final Boolean verdi;
}

public class Filters {
    public static FilterTag of(String categori, Object value) {
        if (value == null) return null;
        if (value instanceof String stringValue) return new FilterTagString(categori, stringValue);
        if (value instanceof Boolean boolValue) return new FilterTagBool(categori, boolValue);
        return null;
    }
}