package no.nav.veilarbaktivitet.stilling_fra_nav;

import no.nav.veilarbaktivitet.stilling_fra_nav.deling_av_cv.Arbeidssted;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.nav.veilarbaktivitet.stilling_fra_nav.DelingAvCvService.utledArbeidstedtekst;

public class DelingAvCvServiceTest {
    @Test
    public void utledAdresser_tom_liste() {
        String result = utledArbeidstedtekst(Collections.emptyList());
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void utledAdresser_mangler_land_og_kommune() {
        Arbeidssted arbeidssted = etArbeidssted(null, null);
        String result = utledArbeidstedtekst(asList(arbeidssted, arbeidssted, arbeidssted));
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void utledAdresser_mangler_land_men_har_kommune() {
        String result = utledArbeidstedtekst(asList(
                etArbeidssted(null, "Molde"),
                etArbeidssted(null, "Stavanger")
        ));
        Assertions.assertThat(result).isEqualTo("Molde, Stavanger");
    }

    @Test
    public void utledAdresser_norge_men_mangler_kommune() {
        Arbeidssted arbeidssted = etArbeidssted("Norge", null);
        String result = utledArbeidstedtekst(singletonList(arbeidssted));
        Assertions.assertThat(result).isEqualTo("Norge");
    }

    @Test
    public void utledAdresser_er_i_utlandet_og_har_kommune() {
        Arbeidssted arbeidssted = etArbeidssted("USA", "Bergen");
        String result = utledArbeidstedtekst(singletonList(arbeidssted));
        Assertions.assertThat(result).isEqualTo("USA");
    }

    @Test
    public void utledAdresser_eksempel() {

        String result = utledArbeidstedtekst(asList(
                etArbeidssted("Sverige", "Oslo"),
                etArbeidssted("Tyskland", null),
                etArbeidssted("Spania", null),
                etArbeidssted(null, null),
                etArbeidssted(null, "Oslo"),
                etArbeidssted("Norge", "Bergen"),
                etArbeidssted("Norge", null)
        ));

        Assertions.assertThat(result).isEqualTo("Sverige, Tyskland, Spania, Oslo, Bergen, Norge");
    }

    @NotNull
    private Arbeidssted etArbeidssted(String land, String kommune) {
        Arbeidssted arbeidssted =
                Arbeidssted.newBuilder()
                        .setAdresse("")
                        .setPostkode("")
                        .setBy("")
                        .setKommune("")
                        .setFylke("")
                        .setLand("")
                        .build();
        arbeidssted.setLand(land);
        arbeidssted.setKommune(kommune);
        return arbeidssted;
    }
}