package no.nav.veilarbaktivitet.kafka.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class KvpAvsluttetKafkaDTO {
    private String aktorId;
    private String avsluttetAv;
    private ZonedDateTime avsluttetDato;
    private String avsluttetBegrunnelse;
}
