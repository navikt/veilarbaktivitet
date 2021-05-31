package no.nav.veilarbaktivitet.avtaltMedNav;

import lombok.Builder;
import lombok.Data;
import no.nav.common.types.identer.AktorId;

import java.util.Date;

@Data
@Builder(toBuilder = true)
public class Forhaandsorientering {

    private String id;
    private Type type;
    private String tekst;
    private Date lestDato;
    private AktorId aktorId;
    private String arenaaktivitetId;
    private String aktivitetId;
    private String aktivitetVersjon;
    private Date opprettetDato;
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
