package no.nav.veilarbaktivitet.aktivitetskort;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import lombok.SneakyThrows;
import no.nav.veilarbaktivitet.person.Person;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 Sub-skjemaer (f.eks. shared-definitions) blir mappet til lokale ressurser via SchemaRegistry.Builder#schemas,
 så testene kan kjøre uten å avhenge av at skjemaene er publisert til GitHub Pages.
 */
class JsonSchemaValidatorTest {

    private static final String SCHEMA_URL_PREFIX =
            "https://navikt.github.io/veilarbaktivitet/schemas/akaas/";
    private static final String SCHEMA_CLASSPATH_PREFIX = "/schemas/akaas/";

    @SneakyThrows
    String convertYamlToJson(InputStream yaml) {
        ObjectMapper yamlReader = YAMLMapper.builder().build();
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = JsonMapper.builder().build();
        return jsonWriter.writeValueAsString(obj);
    }

    /**
     * Mapper $id/$ref-er som peker til GH Pages over til lokale YAML-filer i src/main/resources/schemas/akaas,
     * konvertert til JSON. Returner null for IRIer vi ikke kjenner, slik at standard loader kan ta over.
     */
    private String resolveSchemaLocally(String iri) {
        if (!iri.startsWith(SCHEMA_URL_PREFIX)) {
            return null;
        }
        String filename = iri.substring(SCHEMA_URL_PREFIX.length());
        try (InputStream in = JsonSchemaValidatorTest.class
                .getResourceAsStream(SCHEMA_CLASSPATH_PREFIX + filename)) {
            if (in == null) {
                return null;
            }
            return convertYamlToJson(in);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Klarte ikke lese lokalt skjema: " + filename, e);
        }
    }

    private SchemaRegistry registry() {
        return registry(SchemaRegistryConfig.builder());
    }

    private SchemaRegistry registryWithLocale() {
        return registry(SchemaRegistryConfig.builder().locale(Locale.ENGLISH));
    }

    /**
     * Bygger en registry der eksterne $ref-er løses lokalt, og format-keyword behandles
     * som ren annotasjon (ikke assertion) - i tråd med oppførselen i json-schema-validator 1.5.x.
     */
    private SchemaRegistry registry(SchemaRegistryConfig.Builder configBuilder) {
        SchemaRegistryConfig config = configBuilder
                .formatAssertionsEnabled(false)
                .build();
        return SchemaRegistry.withDefaultDialect(
                SpecificationVersion.DRAFT_7,
                builder -> builder
                        .schemas(this::resolveSchemaLocally)
                        .schemaRegistryConfig(config)
        );
    }

    @Test
    void schema_should_be_in_sync_with_classes() {
        SchemaRegistry registry = registryWithLocale();
        InputStream aktitivitetskortYml = JsonSchemaValidatorTest.class.getResourceAsStream("/schemas/akaas/Aktivitetskort.V1.aktivitetskort.schema.yml");
        String aktiviteskortSchemaJsonString = convertYamlToJson(aktitivitetskortYml);

        Schema jsonSchema = registry.getSchema(aktiviteskortSchemaJsonString);

        var valid = AktivitetskortProducerUtil.validExampleAktivitetskortRecord(Person.fnr("1234567890"));
        var validValidationMessages = jsonSchema.validate(valid);

        assertEquals(0, validValidationMessages.size(), errorMessage(validValidationMessages));

        var invalid = AktivitetskortProducerUtil.invalidExampleRecord(Person.fnr("1234567890"));
        var invalidValidationMessages = jsonSchema.validate(invalid);
        assertEquals(1, invalidValidationMessages.size(), errorMessage(invalidValidationMessages));
        assertEquals("required property 'aktivitetskortType' not found", errorMessage(invalidValidationMessages));
    }

    @Test
    void schema_should_be_in_sync_with_classes_for_kassering() {
        SchemaRegistry registry = registry();
        InputStream kasserYml = JsonSchemaValidatorTest.class.getResourceAsStream("/schemas/akaas/Aktivitetskort.V1.kasser.schema.yml");
        String kasserSchemaJsonString = convertYamlToJson(kasserYml);

        Schema jsonSchema = registry.getSchema(kasserSchemaJsonString);

        var valid = AktivitetskortProducerUtil.validExampleKasseringsRecord();
        var validValidationMessages = jsonSchema.validate(valid);

        assertThat(validValidationMessages).isEmpty();
    }

    @SneakyThrows
    @Test
    void should_validate_with_schema_validExampleFromFile() {
        String json = AktivitetskortProducerUtil.exampleFromFile("validaktivitetskort.json");
        SchemaRegistry registry = registry();
        InputStream aktitivitetskortYml = JsonSchemaValidatorTest.class.getResourceAsStream("/schemas/akaas/Aktivitetskort.V1.aktivitetskort.schema.yml");
        String aktiviteskortSchemaJsonString = convertYamlToJson(aktitivitetskortYml);

        Schema jsonSchema = registry.getSchema(aktiviteskortSchemaJsonString);

        var valid = JsonMapper.builder().build().readTree(json);
        var validValidationMessages = jsonSchema.validate(valid);
        assertThat(validValidationMessages).isEmpty();
    }

    @SneakyThrows
    @Test
    void should_validate_with_schema_validExampleWithZonedDateTime() {
        String json = AktivitetskortProducerUtil.exampleFromFile("validaktivitetskortZonedDatetime.json");
        SchemaRegistry registry = registry();
        InputStream aktitivitetskortYml = JsonSchemaValidatorTest.class.getResourceAsStream("/schemas/akaas/Aktivitetskort.V1.aktivitetskort.schema.yml");
        String aktiviteskortSchemaJsonString = convertYamlToJson(aktitivitetskortYml);

        Schema jsonSchema = registry.getSchema(aktiviteskortSchemaJsonString);

        var valid = JsonMapper.builder().build().readTree(json);
        var validValidationMessages = jsonSchema.validate(valid);
        assertThat(validValidationMessages).isEmpty();
    }

    @SneakyThrows
    @Test
    void should_invalidate_with_schema_validExampleFromFile() {
        String json = AktivitetskortProducerUtil.exampleFromFile("invalidaktivitetskortInvalidActionType.json");
        SchemaRegistry registry = registry();
        InputStream aktitivitetskortYml = JsonSchemaValidatorTest.class.getResourceAsStream("/schemas/akaas/Aktivitetskort.V1.aktivitetskort.schema.yml");
        String aktiviteskortSchemaJsonString = convertYamlToJson(aktitivitetskortYml);

        Schema jsonSchema = registry.getSchema(aktiviteskortSchemaJsonString);

        var valid = JsonMapper.builder().build().readTree(json);
        var validValidationMessages = jsonSchema.validate(valid);
        assertThat(validValidationMessages).hasSize(1);
    }


    private String errorMessage(List<Error> validationErrors) {
        String[] strings = validationErrors.stream().map(Error::getMessage).toArray(String[]::new);
        return String.join("\n", strings);
    }
}
