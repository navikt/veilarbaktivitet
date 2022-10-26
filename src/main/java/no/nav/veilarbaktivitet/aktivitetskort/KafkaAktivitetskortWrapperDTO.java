package no.nav.veilarbaktivitet.aktivitetskort;

import java.time.LocalDateTime;
import java.util.UUID;

public class KafkaAktivitetskortWrapperDTO {
    UUID messageId;
    String source;
    LocalDateTime sendt;
    ActionType actionType;
    AktivitetskortType aktivitetskortType;
    AktivitetskortDTO aktivitetskort;
}
