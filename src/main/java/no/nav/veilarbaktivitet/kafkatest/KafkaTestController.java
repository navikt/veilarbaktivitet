package no.nav.veilarbaktivitet.kafkatest;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbaktivitet.avro.Arbeidssted;
import no.nav.veilarbaktivitet.avro.ForesporselOmDelingAvCv;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/kafka")
@RequiredArgsConstructor
public class KafkaTestController {

	public static String TOPIC="pto.veilarbaktivitet-test-toppic";

	private final KafkaTemplate<String, ForesporselOmDelingAvCv> publisher;


	//This code should not be in prod!
	@PostMapping ("/kafkatest")
	public void kafkaProduceMessage() {
	    ForesporselOmDelingAvCv data = createMelding();
	    String key = data.getBestillingsId();
	    log.info("Skriver til kafka: {}", data);
		publisher.send(TOPIC, key, data);
	}


    static ForesporselOmDelingAvCv createMelding() {
        return ForesporselOmDelingAvCv.newBuilder()
                .setAktorId("AKTORID")
                .setArbeidsgiver("arbeidsgiver")
                .setArbeidssteder(List.of(
                        Arbeidssted.newBuilder()
                                .setAdresse("adresse")
                                .setPostkode("1234")
                                .setKommune("kommune")
                                .setBy("by")
                                .setFylke("fylke")
                                .setLand("land").build(),
                        Arbeidssted.newBuilder()
                                .setAdresse("VillaRosa")
                                .setPostkode(null)
                                .setKommune(null)
                                .setBy(null)
                                .setFylke(null)
                                .setLand("spania").build()))
                .setBestillingsId(UUID.randomUUID().toString())
                .setOpprettet(Instant.now())
                .setOpprettetAv("Z999999")
                .setCallId("callId")
                .setSoknadsfrist("10102021")
                .setStillingsId("stillingsId")
                .setStillingstittel("stillingstittel")
                .setSvarfrist(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();
    }
}
