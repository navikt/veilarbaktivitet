package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.common.json.JsonMapper;
import no.nav.common.json.JsonUtils;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.veilarbaktivitet.SpringBootTestBase;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType;
import no.nav.veilarbaktivitet.aktivitet.dto.TiltakDTO;
import no.nav.veilarbaktivitet.config.kafka.NavCommonKafkaConfig;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
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
import java.util.Objects;
import java.util.UUID;

import static no.nav.veilarbaktivitet.aktivitetskort.IdentType.ARENAIDENT;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AktivitetskortConsumerTest extends SpringBootTestBase {

    @Autowired
    KafkaProducerClient<String, String> producerClient;

    @Value("${topic.inn.aktivitetskort}")
    String topic;

    TiltaksaktivitetDTO tiltaksaktivitetDTO(UUID funksjonellId, AktivitetStatus aktivitetStatus) {
        return TiltaksaktivitetDTO.builder()
                .id(funksjonellId)
                .personIdent(mockBruker.getFnr())
                .startDato(LocalDate.now().minusDays(30))
                .sluttDato(LocalDate.now().minusDays(30))
                .tittel("The Elder Scrolls: Arena")
                .beskrivelse("arenabeskrivelse")
                .aktivitetStatus(aktivitetStatus)
                .endretAv(new IdentDTO("arenaEndretav", ARENAIDENT))
                .endretDato(endretDato)
                .tiltaksNavn("Arendal")
                .tiltaksKode("Arenatiltakskode")
                .arrangoernavn("Arenaarrangørnavn")
                .deltakelseStatus("SOKT_INN")
                .detalj("deltakelsesprosent", "40")
                .detalj("dagerPerUke", "2")
                .build();
    }

    KafkaAktivitetWrapperDTO aktivitetskort(JsonNode payload) {
        return KafkaAktivitetWrapperDTO.builder()
                .messageId(UUID.randomUUID())
                .source("ARENA_TILTAK_AKTIVITET_ACL")
                .sendt(LocalDateTime.now())
                .actionType(ActionType.UPSERT_TILTAK_AKTIVITET_V1)
                .payload(payload)
                .build();
    }

    @Test
    public void happy_case_upsert_ny_tiltaksaktivitet() {
        UUID funksjonellId = UUID.randomUUID();

        TiltaksaktivitetDTO tiltaksaktivitetDTO = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT);
        JsonNode payload = JsonMapper.defaultObjectMapper().valueToTree(tiltaksaktivitetDTO);

        KafkaAktivitetWrapperDTO kafkaAktivitetWrapperDTO =  aktivitetskort(payload);

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, "1", JsonUtils.toJson(kafkaAktivitetWrapperDTO));
        RecordMetadata recordMetadata = producerClient.sendSync(producerRecord);
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> kafkaTestService.erKonsumert(topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, recordMetadata.offset()));

        var aktivitet = aktivitetTestService.hentAktiviteterForFnr(mockBruker)
                .aktiviteter.stream()
                .filter((a) -> Objects.equals(a.getFunksjonellId(), funksjonellId))
                .findFirst();
        Assertions.assertTrue(aktivitet.isPresent());
        Assertions.assertEquals(aktivitet.get().getStatus(), AktivitetStatus.PLANLAGT);
        Assertions.assertEquals(aktivitet.get().getTiltak(), new TiltakDTO(
                tiltaksaktivitetDTO.tiltaksNavn,
                tiltaksaktivitetDTO.arrangoernavn,
                tiltaksaktivitetDTO.deltakelseStatus,
                Integer.parseInt(tiltaksaktivitetDTO.detaljer.get("dagerPerUke")),
                Integer.parseInt(tiltaksaktivitetDTO.detaljer.get("deltakelsesprosent"))
        ));

    }

    @Test
    public void happy_case_upsert_existing_tiltaksaktivitet() throws InterruptedException {
        UUID funksjonellId = UUID.randomUUID();

        TiltaksaktivitetDTO tiltaksaktivitet = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT);
        TiltaksaktivitetDTO tiltaksaktivitetUpdate = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.GJENNOMFORES);

        JsonNode payload = JsonMapper.defaultObjectMapper().valueToTree(tiltaksaktivitet);
        JsonNode payloadUpdate = JsonMapper.defaultObjectMapper().valueToTree(tiltaksaktivitetUpdate);

        KafkaAktivitetWrapperDTO aktivitetskort =  aktivitetskort(payload);
        KafkaAktivitetWrapperDTO aktivitetskortUpdate =  aktivitetskort(payloadUpdate);

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, funksjonellId.toString(), JsonUtils.toJson(aktivitetskort));
        RecordMetadata recordMetadata = producerClient.sendSync(producerRecord);

        ProducerRecord<String, String> producerRecordUpdate = new ProducerRecord<>(topic, funksjonellId.toString(), JsonUtils.toJson(aktivitetskortUpdate));
        RecordMetadata recordMetadataUpdate = producerClient.sendSync(producerRecordUpdate);
        Awaitility.await().atMost(Duration.ofSeconds(1000)).until(() -> kafkaTestService.erKonsumert(topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, recordMetadataUpdate.offset()));

        var aktivitet = aktivitetTestService.hentAktiviteterForFnr(mockBruker)
                .aktiviteter.stream()
                .filter((a) -> Objects.equals(a.getFunksjonellId(), funksjonellId))
                .findFirst();
        Assertions.assertTrue(aktivitet.isPresent());
        Assertions.assertEquals(AktivitetStatus.GJENNOMFORES, aktivitet.get().getStatus());
        Assertions.assertEquals(AktivitetTransaksjonsType.STATUS_ENDRET, aktivitet.get().getTransaksjonsType());
    }

    @Test
    public void duplikat_melding_bare_1_opprettet_transaksjon() {
        UUID funksjonellId = UUID.randomUUID();

        TiltaksaktivitetDTO tiltaksaktivitetDTO = tiltaksaktivitetDTO(funksjonellId, AktivitetStatus.PLANLAGT);
        JsonNode payload = JsonMapper.defaultObjectMapper().valueToTree(tiltaksaktivitetDTO);

        KafkaAktivitetWrapperDTO kafkaAktivitetWrapperDTO =  aktivitetskort(payload);

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, "1", JsonUtils.toJson(kafkaAktivitetWrapperDTO));
        RecordMetadata recordMetadata = producerClient.sendSync(producerRecord);
        RecordMetadata recordMetadataDuplicate = producerClient.sendSync(producerRecord);
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> kafkaTestService.erKonsumert(topic, NavCommonKafkaConfig.CONSUMER_GROUP_ID, recordMetadataDuplicate.offset()));

        var aktiviteter = aktivitetTestService.hentAktiviteterForFnr(mockBruker)
                .aktiviteter.stream()
                .filter((a) -> Objects.equals(a.getFunksjonellId(), funksjonellId))
                .toList();
        Assertions.assertEquals(aktiviteter.size(), 1);
        Assertions.assertEquals(aktiviteter.stream().findFirst().get().getTransaksjonsType(), AktivitetTransaksjonsType.OPPRETTET);
    }

    @Test
    public void endretTidspunkts_skal_settes_fra_melding() {
        // TODO implement
    }

    @Test
    public void fødselsnummer_er_oversatt_til_aktørid() {
        // TODO: implement
    }

    private final MockBruker mockBruker = MockNavService.createHappyBruker();
    private final LocalDateTime endretDato = LocalDateTime.now().minusDays(100);
}
