package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.person.Person;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/*
Testene som bruker schema for å validere, må kjøre etter at schema er publisert til GH pages - github action .github/workflows/asyncApiSchemas.yml
 */
class JsonSchemaValidatorTest {

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
        InputStream aktitivitetskortYml = JsonSchemaValidatorTest.class.getResourceAsStream("/schemas/akaas/Aktivitetskort.V1.aktivitetskort.schema.yml");
        String aktiviteskortSchemaJsonString = convertYamlToJson(aktitivitetskortYml);

        JsonSchema jsonSchema = factory.getSchema(new ByteArrayInputStream(aktiviteskortSchemaJsonString.getBytes()));

        var valid = AktivitetskortProducerUtil.validExampleAktivitetskortRecord(Person.fnr("1234567890"));
        var validValidationMessages = jsonSchema.validate(valid);

        assertEquals(0, validValidationMessages.size(), errorMessage(validValidationMessages));

        var invalid = AktivitetskortProducerUtil.invalidExampleRecord(Person.fnr("1234567890"));
        var invalidValidationMessages = jsonSchema.validate(invalid);
        assertEquals(1, invalidValidationMessages.size(), errorMessage(invalidValidationMessages));
        assertEquals("$.aktivitetskortType: is missing but it is required", errorMessage(invalidValidationMessages));
    }

    @Test
    void schema_should_be_in_sync_with_classes_for_kassering() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        InputStream kasserYml = JsonSchemaValidatorTest.class.getResourceAsStream("/schemas/akaas/Aktivitetskort.V1.kasser.schema.yml");
        String kasserSchemaJsonString = convertYamlToJson(kasserYml);

        JsonSchema jsonSchema = factory.getSchema(new ByteArrayInputStream(kasserSchemaJsonString.getBytes()));

        var valid = AktivitetskortProducerUtil.validExampleKasseringsRecord();
        var validValidationMessages = jsonSchema.validate(valid);

        assertEquals(0, validValidationMessages.size(), errorMessage(validValidationMessages));
    }

    @SneakyThrows
    @Test
    void should_validate_with_schema_validExampleFromFile() {
        String json = AktivitetskortProducerUtil.exampleFromFile("validaktivitetskortZonedDatetime.json");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        InputStream aktitivitetskortYml = JsonSchemaValidatorTest.class.getResourceAsStream("/schemas/akaas/Aktivitetskort.V1.aktivitetskort.schema.yml");
        String aktiviteskortSchemaJsonString = convertYamlToJson(aktitivitetskortYml);

        JsonSchema jsonSchema = factory.getSchema(new ByteArrayInputStream(aktiviteskortSchemaJsonString.getBytes()));

        var valid = new ObjectMapper().readTree(json);
        var validValidationMessages = jsonSchema.validate(valid);
        assertThat(validValidationMessages).isEmpty();
    }

    @SneakyThrows
    @Test
    void should_invalidate_with_schema_validExampleFromFile() {
        String json = AktivitetskortProducerUtil.exampleFromFile("invalidaktivitetskortInvalidActionType.json");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        InputStream aktitivitetskortYml = JsonSchemaValidatorTest.class.getResourceAsStream("/schemas/akaas/Aktivitetskort.V1.aktivitetskort.schema.yml");
        String aktiviteskortSchemaJsonString = convertYamlToJson(aktitivitetskortYml);

        JsonSchema jsonSchema = factory.getSchema(new ByteArrayInputStream(aktiviteskortSchemaJsonString.getBytes()));

        var valid = new ObjectMapper().readTree(json);
        var validValidationMessages = jsonSchema.validate(valid);
        assertThat(validValidationMessages).hasSize(1);
    }


    private String errorMessage(Set<ValidationMessage> validValidationMessages) {
        String[] strings = validValidationMessages.stream().map(ValidationMessage::getMessage).toArray(String[]::new);
        return String.join("\n", strings);
    }
}
