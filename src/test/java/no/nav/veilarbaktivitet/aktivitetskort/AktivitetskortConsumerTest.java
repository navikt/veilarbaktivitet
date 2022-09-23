package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.common.json.JsonMapper;
import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static no.nav.veilarbaktivitet.aktivitetskort.IdentType.ARENAIDENT;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AktivitetskortConsumerTest extends SpringBootTestBase {

    @Autowired
    KafkaProducerClient<String, String> producerClient;

    @Value("${topic.inn.aktivitetskort}")
    String topic;

    @Test
    public void happy_case_upsert_tiltaksaktivitet() throws InterruptedException {
        UUID funksjonellId = UUID.randomUUID();

        TiltaksaktivitetDTO tiltaksaktivitetDTO = TiltaksaktivitetDTO.builder()
                .id(funksjonellId)
                .personIdent(mockBruker.getFnr())
                .startDato(LocalDate.now().minusDays(30))
                .sluttDato(LocalDate.now().minusDays(30))
                .tittel("tittel")
                .beskrivelse("beskrivelse")
                .aktivitetStatus(AktivitetStatus.PLANLAGT)
                .endretAv(new IdentDTO("arenanoe", ARENAIDENT))
                .endretDato(endretDato)
                .tiltaksNavn("Arenakamp")
                .tiltaksKode("FOO")
                .arrangoernavn("arrangørnavn")
                .deltakelseStatus("SOKT_INN")
                .detalj("deltakelsesprosent", "40")
                .detalj("dagerPerUke", "2")
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
//        RecordMetadata recordMetadata2 = producerClient.sendSync(producerRecord);
        System.out.println("test");
        Assertions.assertTrue(true);
    }


    private final MockBruker mockBruker = MockNavService.createHappyBruker();
    private final LocalDateTime endretDato = LocalDateTime.now().minusDays(100);
}
