package no.nav.veilarbaktivitet.aktivitetskort;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@SuperBuilder
@NoArgsConstructor
public class KafkaTiltaksAktivitet extends KafkaAktivitetWrapperDTO {
    TiltaksaktivitetDTO payload;

    @Override
    public UUID funksjonellId() {
        return payload.id;
    }
}
