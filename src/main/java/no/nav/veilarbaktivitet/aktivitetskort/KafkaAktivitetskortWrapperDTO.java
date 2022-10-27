package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;
@Builder
public class KafkaAktivitetskortWrapperDTO {
    UUID messageId;
    String source;
    LocalDateTime sendt;
    ActionType actionType;
    AktivitetskortType aktivitetskortType;
    Aktivitetskort aktivitetskort;
}
