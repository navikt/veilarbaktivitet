package no.nav.veilarbaktivitet.aktivitetskort;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import no.nav.veilarbaktivitet.person.Person;
import org.junit.Test;

import java.time.ZonedDateTime;

import static org.junit.Assert.*;

public class AktivitetsbestillingCreatorTest {

    @Test
    public void schema_should_be_in_sync_with_classes() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema jsonSchema = factory.getSchema(
                AktivitetsbestillingCreatorTest.class.getResourceAsStream("/schemas/AktivitetskortV1.schema.json"));

        var lol = ZonedDateTime.parse("2022-10-19T12:00:00+02:00");

        // var jsonNode = AktivitetskortProducerUtil.validExampleRecord(Person.fnr("1234567890"));
        var jsonNode = AktivitetskortProducerUtil.validExampleFromFile();
        var validationMessages = jsonSchema.validate(jsonNode);
        assertEquals(0, validationMessages.size());
    }

    @Test
    public void should_have_correct_timezone_when_serializing() {
        var jsonNode = AktivitetskortProducerUtil.validExampleRecord(Person.fnr("1234567890"));
        var endretTidspunkt = jsonNode.path("aktivitetskort").get("endretTidspunkt").asText();
        assertEquals("2022-01-01T00:00:00.001+01:00", endretTidspunkt);
    }

}