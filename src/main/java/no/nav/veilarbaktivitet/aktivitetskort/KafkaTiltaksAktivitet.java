package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
public class KafkaTiltaksAktivitet extends KafkaAktivitetWrapperDTO {
    TiltaksaktivitetDTO payload;
}
