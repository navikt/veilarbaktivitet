package no.nav.veilarbaktivitet.aktivitet.dto.filterTags;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FilterTagString implements FilterTag {
    public final String kategori;
    public final String verdi;
}