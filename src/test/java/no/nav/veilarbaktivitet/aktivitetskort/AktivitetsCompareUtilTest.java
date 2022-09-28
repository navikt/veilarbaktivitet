package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class AktivitetsCompareUtilTest {

    @Test
    public void statusendring_skal_være_faktisk_endring() {
        var gammelAktivitet = AktivitetDataTestBuilder.nyTiltaksaktivitet();
        var nyAktivitet = gammelAktivitet
                .withTiltaksaktivitetData(gammelAktivitet
                    .getTiltaksaktivitetData()
                        .withDeltakelseStatus("MOETE")
                );
        Assertions.assertThat(
            AktivitetskortCompareUtil
                .erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isTrue();
    }

    @Test
    public void ider_er_ikke_faktisk_endring() {
        var gammelAktivitet = AktivitetDataTestBuilder.nyTiltaksaktivitet();
        var nyAktivitet = gammelAktivitet
                .withVersjon(10l)
                .withId(12l);
        Assertions.assertThat(
            AktivitetskortCompareUtil
            .erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isFalse();
    }

    @Test
    public void forhåndsorientering_er_ikke_faktisk_endring() {
        var gammelAktivitet = AktivitetDataTestBuilder.nyTiltaksaktivitet();
        var nyAktivitet = gammelAktivitet
                .withForhaandsorientering(AktivitetDataTestBuilder.nyForhaandorientering());
        Assertions.assertThat(
                AktivitetskortCompareUtil
                        .erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isFalse();
    }

}
