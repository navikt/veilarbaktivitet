package no.nav.veilarbaktivitet.varsel.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.varsel.event.VarselEvent;

@Slf4j
public class KafkaVarselMockProducer extends KafkaVarselProducer {

    public KafkaVarselMockProducer() {
        super(null);
    }

    @Override
    public long send(String key, VarselEvent value) {
        log.info("\"Sending\" key/value {}/{}", key, value);
        return 0;
    }

}
