package no.nav.veilarbaktivitet.aktivitetskort.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
@Slf4j
public class JsonUtil {

    private JsonUtil() {
    }

    public static String extractStringPropertyFromJson(String propertyName, String json) throws IOException {
        String result = null;
        JsonFactory jsonFactory = new JsonFactory();
        try (JsonParser parser = jsonFactory.createParser(json)) {
            parser.nextToken();
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = parser.getCurrentName();
                if (propertyName.equals(fieldName) && parser.nextToken() == JsonToken.VALUE_STRING) {
                    result = parser.getValueAsString();
                    break;
                }
            }
        }
        return result;
    }
}
