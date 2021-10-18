package no.nav.veilarbaktivitet.stilling_fra_nav;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.With;

@Builder(toBuilder = true)
@With
@Getter
@Data
public class KontaktpersonData {
    String navn;
    String tittel;
    String mobil;

    public static KontaktpersonDataBuilder builder() {
        return new CustomKontaktpersonDataBuilder();
    }

    public static class CustomKontaktpersonDataBuilder extends KontaktpersonDataBuilder {
        @Override
        public KontaktpersonData build() {
            //returnerer null hvis alle atributer er null
            if (super.navn == null && super.tittel == null && super.mobil == null) {
                return null;
            }

            return super.build();
        }
    }
}
