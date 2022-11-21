package no.nav.veilarbaktivitet.aktivitetskort;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.JSR310DateTimeDeserializerBase;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.key.ZonedDateTimeKeyDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


public class DualDateTimeDeserializer extends JsonDeserializer {
    private ZonedDateTimeKeyDeserializer zonedDateTimeKeyDeserializer = ZonedDateTimeKeyDeserializer.INSTANCE;
    private LocalDateTimeDeserializer localDateTimeDeserializer = new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    @Override
    public ZonedDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        String dateText = jsonParser.getText();
        String localDateTimeRegex = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,6})?$";

        if (dateText.matches(localDateTimeRegex)) {
            return localDateTimeDeserializer.deserialize(jsonParser, deserializationContext).atZone(ZoneId.systemDefault());
        } else {
            return (ZonedDateTime) zonedDateTimeKeyDeserializer.deserializeKey(dateText, deserializationContext);
        }
    }
}
