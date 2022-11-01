package no.nav.veilarbaktivitet.aktivitet.dto.filterTags;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class FilterTagString extends FilterTag {
    public final String kategori;
    public final String verdi;
}