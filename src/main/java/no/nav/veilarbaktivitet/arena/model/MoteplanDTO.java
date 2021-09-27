package no.nav.veilarbaktivitet.arena.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class MoteplanDTO {
    Date startDato; //startKlokkeslett kan også være i denne
    Date sluttDato;
    String sted;
}
