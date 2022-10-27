package no.nav.veilarbaktivitet.aktivitetskort;

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

public class AktivitetsCompareUtilTest {

    @Test
    public void deltakelsestatusendring_skal_være_faktisk_endring() {
        var gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet();
        var nyAktivitet = gammelAktivitet
                .withEksternAktivitetData(gammelAktivitet
                    .getEksternAktivitetData()
                        .withEtiketter(List.of(new Etikett("DELTAR")))
                );
        Assertions.assertThat(
            AktivitetskortCompareUtil
                .erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isTrue();
    }

    @Test
    public void ider_er_ikke_faktisk_endring() {
        var gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet();
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
        var gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet();
        var nyAktivitet = gammelAktivitet
                .withForhaandsorientering(AktivitetDataTestBuilder.nyForhaandorientering());
        Assertions.assertThat(
                AktivitetskortCompareUtil
                        .erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isFalse();
    }

    @Test
    public void aktivitetsstatus_endring_er_ikke_faktisk_endring() {
        var gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
                .withStatus(AktivitetStatus.GJENNOMFORES);
        var nyAktivitet = gammelAktivitet
                .withStatus(AktivitetStatus.FULLFORT);
        Assertions.assertThat(
                AktivitetskortCompareUtil
                        .erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isFalse();
    }

    @Test
    public void tiltaksnavn_endring_er_faktisk_endring() {
        var gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet();
        var nyAktivitet = gammelAktivitet
            .withEksternAktivitetData(gammelAktivitet.getEksternAktivitetData()
                    .withDetaljer(List.of(new Attributt("tiltaksnavn", "Hurra AS")))
            );
        Assertions.assertThat(
            AktivitetskortCompareUtil
                .erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isTrue();
    }

}
