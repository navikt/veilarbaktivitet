package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import no.nav.common.json.JsonMapper;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker;
import no.nav.veilarbaktivitet.mock_nav_modell.MockNavService;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.veilarbaktivitet.aktivitetskort.IdentType.ARENAIDENT;

public class AktivitetskortProducerUtil {
    private static final ObjectMapper objectMapper = JsonUtils.getMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);;
    
    static final MockBruker mockBruker = MockNavService.createHappyBruker();

    public record Pair(String json, UUID messageId) {}

    public static String readFileToString(String path) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(path);
        return asString(resource);
    }

    public static String asString(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonNode aktivitetMessageNode(KafkaAktivitetskortWrapperDTO kafkaAktivitetskortWrapperDTO) {
        return objectMapper
                .valueToTree(kafkaAktivitetskortWrapperDTO);
    }

    public static Pair validExampleRecord() {
        KafkaAktivitetskortWrapperDTO kafkaAktivitetskortWrapperDTO = kafkaAktivitetWrapper();
        JsonNode jsonNode = aktivitetMessageNode(kafkaAktivitetskortWrapperDTO);
        return new Pair(jsonNode.toString(), kafkaAktivitetskortWrapperDTO.messageId);
    }

    public static Pair validExampleFromFile() {
        String json = readFileToString("__files/aktivitetskort/validaktivitetskort.json");
        return new Pair(json, UUID.fromString("2edf9ba0-b195-49ff-a5cd-939c7f26826f"));
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

    @SneakyThrows
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
                .oppgaver(
                        new Oppgaver(
                                new Oppgave("tekst", "subtekst", new URL("http://localhost:8080/ekstern"), "knappetekst"),
                                null)
                )
                .handling(new LenkeSeksjon("tekst", "subtekst", new URL("http://localhost:8080/ekstern"), LenkeType.EKSTERN))
                .handling(new LenkeSeksjon("tekst", "subtekst", new URL("http://localhost:8080/intern"), LenkeType.INTERN))
                .etikett(new Etikett("INNSOKT"))
                .etikett(new Etikett("UTSOKT"))
                .build();

        return KafkaAktivitetskortWrapperDTO
                .builder()
                .messageId(UUID.randomUUID())
                .source(AktivitetsbestillingCreator.ARENA_TILTAK_AKTIVITET_ACL)
                .actionType(ActionType.UPSERT_AKTIVITETSKORT_V1)
                .aktivitetskort(aktivitetskort)
                .aktivitetskortType(AktivitetskortType.ARENA_TILTAK)
                .build();
    }
}
