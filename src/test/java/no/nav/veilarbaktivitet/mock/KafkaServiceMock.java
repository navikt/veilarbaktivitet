package no.nav.veilarbaktivitet.mock;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.aktiviterTilKafka.KafkaAktivitetMelding;
import no.nav.veilarbaktivitet.aktiviterTilKafka.KafkaAktivitetMeldingV2;
import no.nav.veilarbaktivitet.aktiviterTilKafka.KafkaService;

public class KafkaServiceMock implements KafkaService {
    public void sendMelding(KafkaAktivitetMelding melding) {
    }

    public void sendMeldingV2(KafkaAktivitetMeldingV2 meldingV2) {

    }


}
