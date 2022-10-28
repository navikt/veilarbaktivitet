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

    private static JsonNode aktivitetMessageNode(KafkaAktivitetskortWrapperDTO kafkaAktivitetskortWrapperDTO) {
        return JsonUtils.getMapper().valueToTree(kafkaAktivitetskortWrapperDTO);
    }

    public static Pair missingFieldRecord() {
        KafkaAktivitetskortWrapperDTO kafkaAktivitetskortWrapperDTO = kafkaAktivitetWrapper();
        JsonNode jsonNode = aktivitetMessageNode(kafkaAktivitetskortWrapperDTO);
        var payload = (ObjectNode)jsonNode.path("aktivitetskort");
        payload.remove("tittel");
        return new Pair(jsonNode.toString(), kafkaAktivitetskortWrapperDTO.messageId);
    }

    public static Pair extraFieldRecord() {
        KafkaAktivitetskortWrapperDTO kafkaAktivitetskortWrapperDTO = kafkaAktivitetWrapper();
        JsonNode jsonNode = aktivitetMessageNode(kafkaAktivitetskortWrapperDTO);
        var payload = (ObjectNode)jsonNode.path("aktivitetskort");
        payload.put("kake", "123");
        return new Pair(jsonNode.toString(), kafkaAktivitetskortWrapperDTO.messageId);
    }

    public static Pair invalidDateFieldRecord() {
        KafkaAktivitetskortWrapperDTO kafkaAktivitetskortWrapperDTO = kafkaAktivitetWrapper();
        JsonNode jsonNode = aktivitetMessageNode(kafkaAktivitetskortWrapperDTO);
        var payload = (ObjectNode)jsonNode.path("aktivitetskort");
        payload.set("startDato", new TextNode("2022/-1/04T12:00:00+02:00"));
        return new Pair(jsonNode.toString(), kafkaAktivitetskortWrapperDTO.messageId);
    }

    private static KafkaAktivitetskortWrapperDTO kafkaAktivitetWrapper() {
        Aktivitetskort aktivitetskort = Aktivitetskort.builder()
                .id(UUID.randomUUID())
                .personIdent(mockBruker.getFnr())
                .startDato(LocalDate.now().minusDays(30))
                .sluttDato(LocalDate.now().minusDays(30))
                .tittel("The Elder Scrolls: Arena")
                .beskrivelse("arenabeskrivelse")
                .aktivitetStatus(AktivitetStatus.PLANLAGT)
                .endretAv(new IdentDTO("arenaEndretav", ARENAIDENT))
                .endretTidspunkt(LocalDateTime.now().minusDays(100))
                .detalj(new Attributt("deltakelsesprosent", "40"))
                .detalj(new Attributt("dagerPerUke", "2"))
                .build();

        return KafkaAktivitetskortWrapperDTO
                .builder()
                .messageId(UUID.randomUUID())
                .source("ARENA_TILTAK_AKTIVITET_ACL")
                .sendt(LocalDateTime.now())
                .actionType(ActionType.UPSERT_AKTIVITETSKORT_V1)
                .aktivitetskort(aktivitetskort)
                .aktivitetskortType(AktivitetskortType.ARENA_TILTAK)
                .build();
    }
}
