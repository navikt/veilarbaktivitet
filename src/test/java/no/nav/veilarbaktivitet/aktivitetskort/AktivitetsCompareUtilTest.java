package no.nav.veilarbaktivitet.aktivitetskort;

import net.minidev.json.JSONUtil;
import no.nav.common.json.JsonUtils;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus;
import no.nav.veilarbaktivitet.arena.model.AktiviteterDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder;
import org.assertj.core.api.Assertions;
import org.intellij.lang.annotations.Language;
import org.junit.Test;

public class AktivitetsCompareUtilTest {

    @Test
    public void deltakelsestatusendring_skal_være_faktisk_endring() {
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

    @Test
    public void aktivitetsstatus_endring_er_ikke_faktisk_endring() {
        var gammelAktivitet = AktivitetDataTestBuilder.nyTiltaksaktivitet()
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
        var gammelAktivitet = AktivitetDataTestBuilder.nyTiltaksaktivitet();
        var nyAktivitet = gammelAktivitet
            .withTiltaksaktivitetData(gammelAktivitet.getTiltaksaktivitetData()
                .withTiltaksnavn("Nytt navn")
            );
        Assertions.assertThat(
            AktivitetskortCompareUtil
                .erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isTrue();
    }

    @Test
    public void lol() {

        @Language("JSON")
        var json = """
                {
            "tiltaksnavn": "tiltaksnavn",
            "aktivitetId": "123123",
            "tiltakLokaltNavn": "lokaltnavn",
            "arrangor": "arrangor",
            "bedriftsnummer": "asd",
            "deltakelsePeriode": {
                "fom": "2021-11-18",
                "tom": "2021-11-25"
            },
            "deltakelseProsent": 60,
            "deltakerStatus": "GJENN",
            "statusSistEndret": "2021-11-18",
            "begrunnelseInnsoking": "asd",
            "antallDagerPerUke": 3.0
          }
        """;
        var tiltak = JsonUtils.fromJson(json, AktiviteterDTO.Tiltaksaktivitet.class);
        System.out.println(tiltak.getId().id());
    }

}
