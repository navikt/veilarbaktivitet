package no.nav.veilarbaktivitet.mock;

import no.nav.veilarbaktivitet.kafka.KafkaAktivitetMelding;
import no.nav.veilarbaktivitet.kafka.KafkaService;

public class KafkaServiceMock implements KafkaService {
    @Override
    public void sendMelding(KafkaAktivitetMelding melding) {
    }
}
