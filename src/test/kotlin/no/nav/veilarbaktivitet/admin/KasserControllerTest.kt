package no.nav.veilarbaktivitet.admin

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

internal class KasserControllerTest : SpringBootTestBase() {
    private val mockBruker by lazy { navMockService.createHappyBruker(BrukerOptions.happyBruker()) }
    private val veileder by lazy { navMockService.createVeileder(ident = "Z999999", mockBruker = mockBruker) }

    @Test
    fun `skal kke kunne kassere aktivitet uten tilgang`() {
        val aktivitet = aktivitetTestService.opprettAktivitet(
            mockBruker, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.STILLING
            )
        )

        veileder
            .createRequest()
            .put("http://localhost:" + port + "/veilarbaktivitet/api/kassering/" + aktivitet.id)
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun `skal lage ny versjon av aktivitet ved kassering`() {
        val opprettetAktivitet = aktivitetTestService.opprettAktivitet(
            mockBruker, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.STILLING
            )
        )
        val aktivitetId = opprettetAktivitet.id.toLong()
        val versjonerFørKassering = aktivitetTestService.hentVersjoner(aktivitetId.toString(), mockBruker, veileder)
        assertThat(versjonerFørKassering.size).isEqualTo(1)

        aktivitetTestService.kasserAktivitetskort(veileder, opprettetAktivitet.id)

        val versjonerEtterKassering = aktivitetTestService.hentVersjoner(aktivitetId.toString(), mockBruker, veileder)
        assertThat(versjonerEtterKassering.size).isEqualTo(2)
    }

    @Test
    fun `skal overskrive bestemte felter på alle versjoner av aktiviteten når man kasserer`() {
        val opprettetAktivitet = aktivitetTestService.opprettAktivitet(mockBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.STILLING))
        val aktivitetId = opprettetAktivitet.id

        aktivitetTestService.kasserAktivitetskort(veileder, opprettetAktivitet.id)

        val versjoner = aktivitetTestService.hentVersjoner(aktivitetId.toString(), mockBruker, veileder)
        versjoner.forEach { aktivitetVersjon ->
            assertThat(aktivitetVersjon.tittel).isEqualTo("Det var skrevet noe feil, og det er nå slettet")
            assertThat(aktivitetVersjon.avsluttetKommentar).isEqualTo("Kassert av NAV")
            assertThat(aktivitetVersjon.lenke).isEqualTo("Kassert av NAV")
            assertThat(aktivitetVersjon.beskrivelse).isEqualTo("Kassert av NAV")
        }
    }

    @Test
    fun `skal ikke overskrive endret-felter og statusfelt på tidligere versjoner av aktiviteten når man kasserer`() {
        val veilederSomOppretterAktivitet = navMockService.createVeileder(mockBruker = mockBruker)
        val opprettetAktivitet = aktivitetTestService.opprettAktivitet(mockBruker, veilederSomOppretterAktivitet, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.STILLING))
        val aktivitetId = opprettetAktivitet.id

        aktivitetTestService.kasserAktivitetskort(veileder, opprettetAktivitet.id)

        val versjoner = aktivitetTestService.hentVersjoner(aktivitetId.toString(), mockBruker, veileder)
        val sorterteVersjoner = versjoner.sortedBy { it.endretDato }

        val førsteVersjon = sorterteVersjoner.first()
        assertThat(førsteVersjon.status).isEqualTo(AktivitetStatus.PLANLAGT)
        assertThat(førsteVersjon.endretAv).isEqualTo(veilederSomOppretterAktivitet.navIdent)

        val andreVersjon = sorterteVersjoner[1]
        assertThat(andreVersjon.status).isEqualTo(AktivitetStatus.AVBRUTT)
        assertThat(andreVersjon.endretAv).isEqualTo(veileder.navIdent)
    }
}
