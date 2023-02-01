package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.feil.DeserialiseringsFeil;
import no.nav.veilarbaktivitet.aktivitetskort.feil.KeyErIkkeFunksjonellIdFeil;
import no.nav.veilarbaktivitet.aktivitetskort.feil.UgyldigIdentFeil;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class AktivitetsbestillingCreatorTest {
    AktivitetsbestillingCreator aktivitetsbestillingCreator;

    @BeforeEach
    public void setup() {
        PersonService personService = Mockito.mock(PersonService.class);
        Mockito.when(personService.getAktorIdForPersonBruker(ArgumentMatchers.any(Person.class))).thenReturn(Optional.of(Person.aktorId("12345678901")));
        aktivitetsbestillingCreator = new AktivitetsbestillingCreator(personService);
    }

    @SneakyThrows
    String convertYamlToJson(InputStream yaml) {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }



    @Test
    void schema_should_be_in_sync_with_classes() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        InputStream aktitivitetskortYml = AktivitetsbestillingCreatorTest.class.getResourceAsStream("/schemas/Aktivitetskort.V1.aktivitetskort.schema.yml");
        String aktiviteskortSchemaJsonString = convertYamlToJson(aktitivitetskortYml);

        JsonSchema jsonSchema = factory.getSchema(new ByteArrayInputStream(aktiviteskortSchemaJsonString.getBytes()));


        var valid = AktivitetskortProducerUtil.validExampleRecord(Person.fnr("1234567890"));
        var validValidationMessages = jsonSchema.validate(valid);

        assertEquals(0, validValidationMessages.size(), errorMessage(validValidationMessages));

        var invalid = AktivitetskortProducerUtil.invalidExampleRecord(Person.fnr("1234567890"));
        var invalidValidationMessages= jsonSchema.validate(invalid);
        assertEquals(2, invalidValidationMessages.size(), errorMessage(invalidValidationMessages));
    }

    private String errorMessage(Set<ValidationMessage> validValidationMessages) {
        String[] strings = validValidationMessages.stream().map(it -> it.getMessage()).toArray(String[]::new);
        return String.join("\n", strings);
    }

    @Test
    void should_have_correct_timezone_when_serializing() {
        var jsonNode = AktivitetskortProducerUtil.validExampleRecord(Person.fnr("1234567890"));
        var endretTidspunkt = jsonNode.path("aktivitetskort").get("endretTidspunkt").asText();
        assertEquals("2022-01-01T00:00:00.001+01:00", endretTidspunkt);
    }

    @Test
    void should_handle_zoned_datetime_format() throws UgyldigIdentFeil, DeserialiseringsFeil, KeyErIkkeFunksjonellIdFeil {
        String json = AktivitetskortProducerUtil.validExampleFromFile("validaktivitetskortZonedDatetime.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json);
        AktivitetskortBestilling aktivitetskortBestilling = aktivitetsbestillingCreator.lagBestilling(consumerRecord);
        ZonedDateTime expected = ZonedDateTime.of(2022, 10, 19, 12, 0, 0, 0, ZoneId.of("UTC"));
        Assertions.assertThat(aktivitetskortBestilling.getAktivitetskort().endretTidspunkt).isCloseTo(expected, Assertions.within(100, ChronoUnit.MILLIS));
    }

    @Test
    void should_throw_exception_when_kafka_key_is_not_equal_to_funksjonell_id() throws UgyldigIdentFeil, DeserialiseringsFeil, KeyErIkkeFunksjonellIdFeil {
        String json = AktivitetskortProducerUtil.validExampleFromFile("validaktivitetskortZonedDatetime.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "invalid-key", json);
        assertThrows(KeyErIkkeFunksjonellIdFeil.class, () -> {
            aktivitetsbestillingCreator.lagBestilling(consumerRecord);
        });
    }

    @Test
    void should_handle_zoned_datetime_format_pluss_time() throws UgyldigIdentFeil, DeserialiseringsFeil, KeyErIkkeFunksjonellIdFeil {
        String json = AktivitetskortProducerUtil.validExampleFromFile("validaktivitetskortZonedDatetime+Time.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json);
        AktivitetskortBestilling aktivitetskortBestilling = aktivitetsbestillingCreator.lagBestilling(consumerRecord);
        ZonedDateTime expected = ZonedDateTime.of(2022, 10, 19, 11, 0, 0, 0, ZoneId.of("UTC"));
        Assertions.assertThat(aktivitetskortBestilling.getAktivitetskort().endretTidspunkt).isCloseTo(expected, Assertions.within(100, ChronoUnit.MILLIS));

    }

    @Test
    void should_handle_UNzoned_datetime_format() throws UgyldigIdentFeil, DeserialiseringsFeil, KeyErIkkeFunksjonellIdFeil {
        String json = AktivitetskortProducerUtil.validExampleFromFile("validaktivitetskortUnzonedDatetime.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "56155242-6481-43b5-9eac-4d7af695bf9d", json);
        AktivitetskortBestilling aktivitetskortBestilling = aktivitetsbestillingCreator.lagBestilling(consumerRecord);
        ZonedDateTime expected = ZonedDateTime.of(2022, 10, 19, 12, 0, 0, 0, ZoneId.of("Europe/Oslo"));
        var endretTidspunkt = aktivitetskortBestilling.getAktivitetskort().getEndretTidspunkt();
        Assertions.assertThat(endretTidspunkt).isCloseTo(expected, Assertions.within(100, ChronoUnit.MILLIS));
    }
}
