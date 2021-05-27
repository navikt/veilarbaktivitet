package no.nav.veilarbaktivitet.avtaltMedNav;

import lombok.Builder;
import lombok.Data;
import no.nav.common.types.identer.AktorId;
import org.joda.time.DateTime;

@Data
@Builder(toBuilder = true)
public class Forhaandsorientering {

    private String id;
    private Type type;
    private String tekst;
    private DateTime lestDato;
    private AktorId aktorId;
    private String arenaaktivitetId;
    private String aktivitetId;
    private String aktivitetVersjon;
    private DateTime opprettetDato;
    private String opprettetAv;


    public static ForhaandsorienteringBuilder builder() {
        return new CustomForhaandsorienteringBuilder();
    }

    public ForhaandsorienteringDTO toDTO() {
        return new ForhaandsorienteringDTO(id, type, tekst, lestDato).toBuilder().build();
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
