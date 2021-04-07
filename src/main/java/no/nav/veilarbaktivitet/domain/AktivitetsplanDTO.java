package no.nav.veilarbaktivitet.domain;

import static java.util.Collections.emptyList;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AktivitetsplanDTO {
	public List<AktivitetDTO> aktiviteter = emptyList();
}
