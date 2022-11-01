package no.nav.veilarbaktivitet.aktivitet.dto.filterTags;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class FilterTagBool extends FilterTag {
    public final String kategori;
    public final Boolean verdi;
}