package no.nav.fo.veilarbaktivitet.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AktivitetTagDTO {
    public String type;
    public String tag;
}
