package no.nav.veilarbaktivitet.send_paa_kafka;

import lombok.Builder;
import lombok.Data;
import lombok.With;

@Data
@Builder
@With
public class JobbDTO {
    private long id;
    private JobbType jobbType;
    private long aktivitetId;
    private long versjon;
    private Status status;
}

