package no.nav.veilarbaktivitet.aktivitet.dto.filterTags;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FilterTagBool implements FilterTag {
    public final String kategori;
    public final Boolean verdi;
}