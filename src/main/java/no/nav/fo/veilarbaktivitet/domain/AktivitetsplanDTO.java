package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

import static java.util.Collections.emptyList;

@Data
@Accessors(chain = true)
public class AktivitetsplanDTO {
    public List<AktivitetDTO> aktiviteter = emptyList();
}
