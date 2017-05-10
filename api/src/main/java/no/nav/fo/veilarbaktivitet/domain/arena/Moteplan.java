package no.nav.fo.veilarbaktivitet.domain.arena;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class Moteplan {
    Date startDato; //startKlokkeslett kan også være i denne
    Date sluttDato;
    String sted;
}
