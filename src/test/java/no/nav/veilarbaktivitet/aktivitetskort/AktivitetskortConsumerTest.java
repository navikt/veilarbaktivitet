package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.common.json.JsonMapper;
import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.awaitility.Awaitility;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AktivitetskortConsumerTest extends SpringBootTestBase {

    @Autowired
    KafkaProducerClient<String, String> producerClient;

    @Value("${topic.inn.aktivitetskort}")
    String topic;

    @Test
    public void happy_case_upsert_tiltaksaktivitet() {
        UUID funksjonellId = UUID.randomUUID();

        TiltaksaktivitetDTO tiltaksaktivitetDTO = TiltaksaktivitetDTO.builder()
                .funksjonellId(funksjonellId)
                .personIdent("A123456")
                .tittel("tittel")
                .startDato(Date.from(Instant.now().minus(30, ChronoUnit.DAYS)))
                .sluttDato(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .beskrivelse("beskrivelse")
                .statusDTO(new StatusDTO(AktivitetStatus.PLANLAGT, "aarsak"))
                .tiltakDTO(new TiltakDTO("kode", "navn"))
                .arrangornavn("arrangornavn")
                .deltakelsesprosent(40)
                .dagerPerUke(2)
                .registrertDato(Date.from(Instant.now()))
                .statusEndretDato(Date.from(Instant.now()))
                .build();
        JsonNode payload = JsonMapper.defaultObjectMapper().valueToTree(tiltaksaktivitetDTO);

        AktivitetskortDTO aktivitetskortDTO = AktivitetskortDTO.builder()
                .id(UUID.randomUUID())
                .utsender("ARENA_TILTAK_AKTIVITET_ACL")
                .sendt(LocalDate.now())
                .actionType(ActionType.UPSERT_TILTAK_AKTIVITET_V1)
                .payload(payload)
                .build();

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, "1", JsonUtils.toJson(aktivitetskortDTO));
        RecordMetadata recordMetadata = producerClient.sendSync(producerRecord);
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> kafkaTestService.erKonsumert(topic, "veilarbaktivitet-consumer", recordMetadata.offset()));

        System.out.println("test");
        Assertions.assertTrue(true);
    }
}
