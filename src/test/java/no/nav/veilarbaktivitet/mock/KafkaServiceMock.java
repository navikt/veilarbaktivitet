package no.nav.veilarbaktivitet.mock;

import no.nav.veilarbaktivitet.aktiviterTilKafka.KafkaAktivitetMeldingV3;
import no.nav.veilarbaktivitet.aktiviterTilKafka.KafkaService;

public class KafkaServiceMock implements KafkaService {
    public long sendMelding(KafkaAktivitetMeldingV3 meldingV3) {
        return 0L;
    }
}
