package no.nav.veilarbaktivitet.varsel;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.varsel.event.*;
import no.nav.veilarbaktivitet.varsel.exceptions.VarselException;
import no.nav.veilarbaktivitet.varsel.kafka.KafkaVarselProducer;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class VarselService {
    private final KafkaVarselProducer kafkaVarselProducer;

    static final String FODSELSNUMMER_AREMARK = "10108003980";

    /**
     * - Når man setter forhåndsorientering på en aktivitet skal det sendes et varsel etter 30 min, om den ikke er lest
     */
    public VarselEvent sendVarsler() {

        final CreateVarselPayload testEvent = CreateVarselPayload.builder()
                .event(VarselEventType.CREATE)
                .id(UUID.randomUUID().toString())
                .type(VarselType.MELDING)
                .fodselsnummer(FODSELSNUMMER_AREMARK)
                .groupId("TEST_GROUP")
                .message("Dette er en test fra NAV, venligst se bort fra denne beskjeden")
                .link("https://nav.no")
                .build();

        try {
            kafkaVarselProducer.send(FODSELSNUMMER_AREMARK, testEvent);
            return testEvent;
        } catch (Exception e) {
            log.error("Det skjedde en feil ved sending av varsel med transactionId {}", testEvent.getTransactionId(), e);
            throw new VarselException("Det skjedde en feil ved sending av varsel med transactionId " + testEvent.getTransactionId(), e);
        }
    }

    public VarselEvent sendStopp(String id) {
        final VarselEvent testDoneEvent = VarselDonePayload.builder()
                .event(VarselEventType.DONE)
                .id(id)
                .fodselsnummer(FODSELSNUMMER_AREMARK)
                .groupId("TEST_GROUP")
                .build();

        try {
            kafkaVarselProducer.send(FODSELSNUMMER_AREMARK, testDoneEvent);
            return testDoneEvent;
        } catch (Exception e) {
            log.error("Det skjedde en feil ved sending av Done event med transactionId {}", testDoneEvent.getTransactionId(), e);
            throw new VarselException("Det skjedde en feil ved sending av Done event med transactionId " + testDoneEvent.getTransactionId(), e);
        }
    }

}
