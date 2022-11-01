package no.nav.veilarbaktivitet.aktivitet.dto.filterTags;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class FilterTagBool extends FilterTag {
    public final String kategori;
    public final Boolean verdi;
}