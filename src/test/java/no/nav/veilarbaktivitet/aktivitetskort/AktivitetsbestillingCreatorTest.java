package no.nav.veilarbaktivitet.aktivitetskort;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import no.nav.veilarbaktivitet.aktivitetskort.bestilling.AktivitetskortBestilling;
import no.nav.veilarbaktivitet.aktivitetskort.feil.DeserialiseringsFeil;
import no.nav.veilarbaktivitet.aktivitetskort.feil.UgyldigIdentFeil;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.PersonService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.Assert.*;

public class AktivitetsbestillingCreatorTest {


    AktivitetsbestillingCreator aktivitetsbestillingCreator;

    @Before
    public void setup(){
        PersonService personService = Mockito.mock(PersonService.class);
        Mockito.when(personService.getAktorIdForPersonBruker(ArgumentMatchers.any(Person.class))).thenReturn(Optional.of(Person.aktorId("12345678901")));
        aktivitetsbestillingCreator = new AktivitetsbestillingCreator(personService);
    }

    @Test
    public void schema_should_be_in_sync_with_classes() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema jsonSchema = factory.getSchema(
                AktivitetsbestillingCreatorTest.class.getResourceAsStream("/schemas/AktivitetskortV1.schema.json"));

        var jsonNode = AktivitetskortProducerUtil.validExampleRecord(Person.fnr("1234567890"));
        var validationMessages = jsonSchema.validate(jsonNode);
        assertEquals(0, validationMessages.size());
    }

    @Test
    public void should_have_correct_timezone_when_serializing() {
        var jsonNode = AktivitetskortProducerUtil.validExampleRecord(Person.fnr("1234567890"));
        var endretTidspunkt = jsonNode.path("aktivitetskort").get("endretTidspunkt").asText();
        assertEquals("2022-01-01T00:00:00.001+01:00", endretTidspunkt);
    }

    @Test
    public void should_handle_zoned_datetime_format() throws UgyldigIdentFeil, DeserialiseringsFeil {
        String json = AktivitetskortProducerUtil.validExampleFromFile("validaktivitetskortZonedDatetime.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", json);
        AktivitetskortBestilling aktivitetskortBestilling = aktivitetsbestillingCreator.lagBestilling(consumerRecord);
        ZonedDateTime expected = ZonedDateTime.of(2022, 10, 19, 12, 0, 0, 0, ZoneId.of("UTC"));
        Assertions.assertThat(aktivitetskortBestilling.getAktivitetskort().endretTidspunkt).isCloseTo(expected, Assertions.within(100, ChronoUnit.MILLIS));

    }

    @Test
    public void should_handle_UNzoned_datetime_format() throws UgyldigIdentFeil, DeserialiseringsFeil {
        String json = AktivitetskortProducerUtil.validExampleFromFile("validaktivitetskortUnzonedDatetime.json");
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", json);
        AktivitetskortBestilling aktivitetskortBestilling = aktivitetsbestillingCreator.lagBestilling(consumerRecord);
        ZonedDateTime expected = ZonedDateTime.of(2022, 10, 19, 12, 0, 0, 0, ZoneId.of("Europe/Oslo"));
        var endretTidspunkt = aktivitetskortBestilling.getAktivitetskort().getEndretTidspunkt();
        Assertions.assertThat(endretTidspunkt).isCloseTo(expected, Assertions.within(100, ChronoUnit.MILLIS));
    }
}