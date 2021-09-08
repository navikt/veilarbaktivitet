package no.nav.veilarbaktivitet.oppfolging;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class OppfolgingAvsluttetKafkaDTO extends JsonMelding {
    private String aktorId;
    private ZonedDateTime sluttdato;
}
