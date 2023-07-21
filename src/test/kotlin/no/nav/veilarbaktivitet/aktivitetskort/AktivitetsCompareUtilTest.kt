package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortCompareUtil.erFaktiskOppdatert
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

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
        assertThat(
            erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isTrue()
    }

    @Test
    fun ider_er_ikke_faktisk_endring() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
        val nyAktivitet = gammelAktivitet
            .withVersjon(10L)
            .withId(12L)
        assertThat(
            erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isFalse()
    }

    @Test
    fun forhåndsorientering_er_ikke_faktisk_endring() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
        val nyAktivitet = gammelAktivitet
            .withForhaandsorientering(AktivitetDataTestBuilder.nyForhaandorientering())
        assertThat(
            erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isFalse()
    }

    @Test
    fun aktivitetsstatus_endring_er_ikke_faktisk_endring() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
            .withStatus(AktivitetStatus.GJENNOMFORES)
        val nyAktivitet = gammelAktivitet
            .withStatus(AktivitetStatus.FULLFORT)
        assertThat(
            erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isFalse()
    }

    @Test
    fun endretTidspunkt_og_endretAv_er_ikke_faktisk_endring() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
            .withEndretAvType(Innsender.NAV)
        val nyEndretAaAktivitet = gammelAktivitet
            .withEndretAv("Hei")
        val nyEndretDatoAktivitet = gammelAktivitet
            .withEndretDato(Date.from(ZonedDateTime.now().minusDays(10).toInstant()))
        val nyEndretTypeAktivitet = gammelAktivitet
            .withEndretAvType(Innsender.ARBEIDSGIVER)
        assertThat(erFaktiskOppdatert(gammelAktivitet, nyEndretAaAktivitet)).isFalse()
        assertThat(erFaktiskOppdatert(gammelAktivitet, nyEndretDatoAktivitet)).isFalse()
        assertThat(erFaktiskOppdatert(gammelAktivitet, nyEndretTypeAktivitet)).isFalse()
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
        assertThat(
            erFaktiskOppdatert(gammelAktivitet, nyAktivitet)
        ).isTrue()
    }
}
