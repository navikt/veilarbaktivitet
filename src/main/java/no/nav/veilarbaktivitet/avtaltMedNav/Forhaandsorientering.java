package no.nav.veilarbaktivitet.avtaltMedNav;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Date;

@Data
@Builder(toBuilder = true)
public class Forhaandsorientering {

    @RequiredArgsConstructor
    public enum Type {
        SEND_FORHAANDSORIENTERING("send_forhandsorientering"),
        SEND_PARAGRAF_11_9("send_paragraf_11_9"),
        IKKE_SEND_FORHAANDSORIENTERING("ikke_send_forhandsorientering");

        private final String value;
    }

    private Type type;
    private String tekst;
    private Date lest;

    public static ForhaandsorienteringBuilder builder() {
        return new CustomForhaandsorienteringBuilder();
    }

    public static class CustomForhaandsorienteringBuilder extends ForhaandsorienteringBuilder {
        @Override
        public Forhaandsorientering build() {
            if (super.type == null) {
                return null;
            }

            return super.build();
        }
    }
}
