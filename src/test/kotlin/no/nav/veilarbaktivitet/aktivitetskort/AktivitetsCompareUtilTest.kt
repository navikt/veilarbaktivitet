package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class AktivitetsCompareUtilTest {
    @Test
    fun deltakelsestatusendring_skal_være_faktisk_endring() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
        val nyAktivitet = gammelAktivitet
            .withEksternAktivitetData(
                gammelAktivitet
                    .getEksternAktivitetData()
                    .copy(etiketter = listOf(
                        Etikett(
                            "DELTAR"
                        )
                    ))
            )
        Assertions.assertThat(
            AktivitetskortCompareUtil
                .erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isTrue()
    }

    @Test
    fun ider_er_ikke_faktisk_endring() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
        val nyAktivitet = gammelAktivitet
            .withVersjon(10L)
            .withId(12L)
        Assertions.assertThat(
            AktivitetskortCompareUtil
                .erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isFalse()
    }

    @Test
    fun forhåndsorientering_er_ikke_faktisk_endring() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
        val nyAktivitet = gammelAktivitet
            .withForhaandsorientering(AktivitetDataTestBuilder.nyForhaandorientering())
        Assertions.assertThat(
            AktivitetskortCompareUtil
                .erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isFalse()
    }

    @Test
    fun aktivitetsstatus_endring_er_ikke_faktisk_endring() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
            .withStatus(AktivitetStatus.GJENNOMFORES)
        val nyAktivitet = gammelAktivitet
            .withStatus(AktivitetStatus.FULLFORT)
        Assertions.assertThat(
            AktivitetskortCompareUtil
                .erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isFalse()
    }

    @Test
    fun tiltaksnavn_endring_er_faktisk_endring() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
        val nyeDetaljer = gammelAktivitet.getEksternAktivitetData()
            .copy(detaljer = listOf(
                Attributt(
                    "tiltaksnavn",
                    "Hurra AS"
                )
            ))
        val nyAktivitet = gammelAktivitet.withEksternAktivitetData(nyeDetaljer)
        Assertions.assertThat(
            AktivitetskortCompareUtil.erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isTrue()
    }
}
