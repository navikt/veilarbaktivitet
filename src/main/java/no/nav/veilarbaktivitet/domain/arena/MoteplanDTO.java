package no.nav.veilarbaktivitet.domain.arena;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class MoteplanDTO {
    ZonedDateTime startDato; //startKlokkeslett kan også være i denne
    ZonedDateTime sluttDato;
    String sted;
}
