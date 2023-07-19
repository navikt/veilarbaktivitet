package no.nav.veilarbaktivitet.aktivitetskort.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {

    @Test
    void extractFieldFromJson() throws IOException {

        String someJson = """
                {
                  "messageId": "2edf9ba0-b195-49ff-a5cd-939c7f26826f",
                  "source": "TEAM_TILTAK",
                  "actionType": "UPSERT_AKTIVITETSKORT_V1",
                  "aktivitetskortType": "MIDLERTIDIG_LONNSTILSKUDD",
                  "aktivitetskort": {}
                }
                """;

        String messageId = JsonUtil.extractStringPropertyFromJson("messageId", someJson);
        assertThat(messageId).isEqualTo("2edf9ba0-b195-49ff-a5cd-939c7f26826f");

    }

    @Test
    void invalidJsonYieldsNull() {
        assertThrows(IOException.class,
                () -> {
                        String invalidJson = """
                    
                      "messageId": "2edf9ba0-b195-49ff-a5cd-939c7f26826f",
                      "source": "TEAM_TILTAK",
                      "actionType": "UPSERT_AKTIVITETSKORT_V1",
                      "aktivitetskortType": "MIDLERTIDIG_LONNSTILSKUDD",
                      "aktivitetskort": {}
                    
                    """;
                        JsonUtil.extractStringPropertyFromJson("messageId", invalidJson);
                });
    }

    @Test
    void missingPropertyYieldsNull() throws IOException {
        String missingProperty = """
                {
                  "source": "TEAM_TILTAK",
                  "actionType": "UPSERT_AKTIVITETSKORT_V1",
                  "aktivitetskortType": "MIDLERTIDIG_LONNSTILSKUDD",
                  "aktivitetskort": {}
                }
                """;
        String missingPropertyResult = JsonUtil.extractStringPropertyFromJson("messageId", missingProperty);
        assertThat(missingPropertyResult).isNull();
    }

    @Test
    void nestedElementsIsOk() throws IOException {
        String nestedElement = """
                {
                  "source": "TEAM_TILTAK",
                  "actionType": "UPSERT_AKTIVITETSKORT_V1",
                  "aktivitetskortType": "MIDLERTIDIG_LONNSTILSKUDD",
                  "aktivitetskort": {
                    "someProp" : true,
                    "messageId": "2edf9ba0-b195-49ff-a5cd-939c7f26826f"
                  }
                }
                """;
        String nestedElementResult = JsonUtil.extractStringPropertyFromJson("messageId", nestedElement);
        assertThat(nestedElementResult).isEqualTo("2edf9ba0-b195-49ff-a5cd-939c7f26826f");
    }

    @Test
    void elementIsObject() throws IOException {
        String nestedElement = """
                {
                  "source": "TEAM_TILTAK",
                  "actionType": "UPSERT_AKTIVITETSKORT_V1",
                  "aktivitetskortType": "MIDLERTIDIG_LONNSTILSKUDD",
                  "messageId": {
                    "someProp" : true,
                    "otherProp": "OK"
                  }
                }
                """;
        String nestedElementResult = JsonUtil.extractStringPropertyFromJson("messageId", nestedElement);
        assertThat(nestedElementResult).isNull();
    }

    @Test
    void elementNotStringType() throws IOException {
        String nestedElement = """
                {
                  "source": "TEAM_TILTAK",
                  "actionType": "UPSERT_AKTIVITETSKORT_V1",
                  "aktivitetskortType": "MIDLERTIDIG_LONNSTILSKUDD",
                  "messageId": true
                }
                """;
        String nestedElementResult = JsonUtil.extractStringPropertyFromJson("messageId", nestedElement);
        assertThat(nestedElementResult).isNull();
    }

}