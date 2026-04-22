package no.nav.veilarbaktivitet.aktivitetskort

import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.AktivitetMuterbareFelter
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.Eksternaktivitet
import no.nav.veilarbaktivitet.aktivitet.domain.aktiviteter.SporingsData
import no.nav.veilarbaktivitet.aktivitetskort.AktivitetskortCompareUtil.erFaktiskOppdatert
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Attributt
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Etikett
import no.nav.veilarbaktivitet.aktivitetskort.dto.aktivitetskort.Sentiment
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

internal class AktivitetsCompareUtilTest {

    private fun AktivitetData.toEndre(): Eksternaktivitet.Endre {
        return Eksternaktivitet.Endre(
            id = id,
            versjon = versjon,
            muterbareFelter = AktivitetMuterbareFelter(
                tittel = tittel,
                beskrivelse = beskrivelse,
                fraDato = fraDato,
                tilDato = tilDato,
                lenke = lenke,
            ),
            sporing = SporingsData(
                endretAv = endretAv,
                endretAvType = endretAvType,
                endretDato = ZonedDateTime.now(),
            ),
            eksternAktivitetData = eksternAktivitetData,
            erAvtalt = isAvtalt,
            status = status,
        )
    }

    @Test
    fun deltakelsestatusendring_skal_være_faktisk_endring() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
        val nyAktivitet = gammelAktivitet.toEndre().copy(
            eksternAktivitetData = gammelAktivitet.eksternAktivitetData.copy(
                etiketter = listOf(
                    Etikett("Deltar", Sentiment.POSITIVE, "DELTAR")
                )
            )
        )
        assertThat(erFaktiskOppdatert(nyAktivitet, gammelAktivitet)).isTrue()
    }

    @Test
    fun ider_er_ikke_faktisk_endring() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
        val nyAktivitet = gammelAktivitet.toEndre().copy(
            versjon = 10L,
            id = 12L,
        )
        assertThat(erFaktiskOppdatert(nyAktivitet, gammelAktivitet)).isFalse()
    }

    @Test
    fun `forhåndsorientering er ikke faktisk endring`() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
            .withForhaandsorientering(AktivitetDataTestBuilder.nyForhaandorientering())
        val nyAktivitet = gammelAktivitet.toEndre()
        assertThat(erFaktiskOppdatert(nyAktivitet, gammelAktivitet)).isFalse()
    }

    @Test
    fun `aktivitetsstatus endring er ikke faktisk endring fordi dette skjer i egne endepunkter og transaksjoner`() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
            .withStatus(AktivitetStatus.GJENNOMFORES)
        val nyAktivitet = gammelAktivitet.toEndre().copy(
            status = AktivitetStatus.FULLFORT,
        )
        assertThat(erFaktiskOppdatert(nyAktivitet, gammelAktivitet)).isFalse()
    }

    @Test
    fun endretTidspunkt_og_endretAv_er_ikke_faktisk_endring() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
            .withEndretAvType(Innsender.NAV)
        val base = gammelAktivitet.toEndre()
        val nyEndretAaAktivitet = base.copy(sporing = base.sporing.copy(endretAv = "Hei"))
        val nyEndretDatoAktivitet = base.copy(sporing = base.sporing.copy(endretDato = ZonedDateTime.now().minusDays(10)))
        val nyEndretTypeAktivitet = base.copy(sporing = base.sporing.copy(endretAvType = Innsender.ARBEIDSGIVER))
        assertThat(erFaktiskOppdatert(nyEndretAaAktivitet, gammelAktivitet)).isFalse()
        assertThat(erFaktiskOppdatert(nyEndretDatoAktivitet, gammelAktivitet)).isFalse()
        assertThat(erFaktiskOppdatert(nyEndretTypeAktivitet, gammelAktivitet)).isFalse()
    }

    @Test
    fun tiltaksnavn_endring_er_faktisk_endring() {
        val gammelAktivitet = AktivitetDataTestBuilder.nyEksternAktivitet()
        val nyAktivitet = gammelAktivitet.toEndre().copy(
            eksternAktivitetData = gammelAktivitet.eksternAktivitetData.copy(
                detaljer = listOf(
                    Attributt("tiltaksnavn", "Hurra AS")
                )
            )
        )
        assertThat(erFaktiskOppdatert(nyAktivitet, gammelAktivitet)).isTrue()
    }
}
