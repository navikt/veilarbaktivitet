package no.nav.veilarbaktivitet.oppfolging.avslutt;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
class OppfolgingAvsluttetKafkaDTO {
    private String aktorId;
    private ZonedDateTime sluttdato;
}
