package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static no.nav.veilarbaktivitet.aktivitetskort.IdentType.ARENAIDENT;

public class AktivitetskortProducerUtil {

    static final MockBruker mockBruker = MockNavService.createHappyBruker();

    public record Pair(String json, UUID messageId) {}

    private static JsonNode aktivitetMessageNode(KafkaAktivitetWrapperDTO kafkaAktivitetWrapperDTO) {
        return JsonUtils.getMapper().valueToTree(kafkaAktivitetWrapperDTO);
    }

    public static Pair missingFieldRecord() {
        KafkaAktivitetWrapperDTO kafkaAktivitetWrapperDTO = kafkaAktivitetWrapper();
        JsonNode jsonNode = aktivitetMessageNode(kafkaAktivitetWrapperDTO);
        var payload = (ObjectNode)jsonNode.path("payload");
        payload.remove("tittel");
        return new Pair(jsonNode.toString(), kafkaAktivitetWrapperDTO.messageId);
    }

    public static Pair extraFieldRecord() {
        KafkaAktivitetWrapperDTO kafkaAktivitetWrapperDTO = kafkaAktivitetWrapper();
        JsonNode jsonNode = aktivitetMessageNode(kafkaAktivitetWrapperDTO);
        var payload = (ObjectNode)jsonNode.path("payload");
        payload.put("kake", "123");
        return new Pair(jsonNode.toString(), kafkaAktivitetWrapperDTO.messageId);
    }

    public static Pair invalidDateFieldRecord() {
        KafkaAktivitetWrapperDTO kafkaAktivitetWrapperDTO = kafkaAktivitetWrapper();
        JsonNode jsonNode = aktivitetMessageNode(kafkaAktivitetWrapperDTO);
        var payload = (ObjectNode)jsonNode.path("payload");
        payload.set("startDato", new TextNode("2022/-1/04T12:00:00+02:00"));
        return new Pair(jsonNode.toString(), kafkaAktivitetWrapperDTO.messageId);
    }

    private static KafkaAktivitetWrapperDTO kafkaAktivitetWrapper() {
        TiltaksaktivitetDTO tiltaksaktivitetDTO = TiltaksaktivitetDTO.builder()
                .id(UUID.randomUUID())
                .personIdent(mockBruker.getFnr())
                .startDato(LocalDate.now().minusDays(30))
                .sluttDato(LocalDate.now().minusDays(30))
                .tittel("The Elder Scrolls: Arena")
                .beskrivelse("arenabeskrivelse")
                .aktivitetStatus(AktivitetStatus.PLANLAGT)
                .endretAv(new IdentDTO("arenaEndretav", ARENAIDENT))
                .endretDato(LocalDateTime.now().minusDays(100))
                .tiltaksNavn("Arendal")
                .tiltaksKode("Arenatiltakskode")
                .arrangoernavn("Arenaarrangørnavn")
                .deltakelseStatus("SOKT_INN")
                .detalj("deltakelsesprosent", "40")
                .detalj("dagerPerUke", "2")
                .build();

        return KafkaTiltaksAktivitet.builder()
                .messageId(UUID.randomUUID())
                .source("ARENA_TILTAK_AKTIVITET_ACL")
                .sendt(LocalDateTime.now())
                .actionType(ActionType.UPSERT_TILTAK_AKTIVITET_V1)
                .payload(tiltaksaktivitetDTO)
                .build();
    }
}
