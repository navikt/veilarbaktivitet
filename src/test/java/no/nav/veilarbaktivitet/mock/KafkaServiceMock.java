package no.nav.veilarbaktivitet.mock;

import no.nav.veilarbaktivitet.aktiviterTilKafka.KafkaAktivitetMeldingV4;
import no.nav.veilarbaktivitet.aktiviterTilKafka.KafkaService;

public class KafkaServiceMock implements KafkaService {
    @Override
    public long sendMelding(KafkaAktivitetMeldingV4 meldingV4) {
        return 0L;
    }

}
