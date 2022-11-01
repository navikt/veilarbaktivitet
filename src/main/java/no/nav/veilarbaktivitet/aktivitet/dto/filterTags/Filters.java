package no.nav.veilarbaktivitet.aktivitet.dto.filterTags;

public class Filters {
    public static FilterTag of(String categori, Object value) {
        if (value == null) return null;
        if (value instanceof String stringValue) return new FilterTagString(categori, stringValue);
        if (value instanceof Boolean boolValue) return new FilterTagBool(categori, boolValue);
        return null;
    }
}
