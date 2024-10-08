package no.nav.veilarbaktivitet.admin

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTransaksjonsType.KASSERT
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.mock_nav_modell.BrukerOptions
import no.nav.veilarbaktivitet.mock_nav_modell.MockBruker
import no.nav.veilarbaktivitet.mock_nav_modell.MockVeileder
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.HttpStatus
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KasserControllerTest : SpringBootTestBase() {

    private lateinit var mockBruker: MockBruker
    private lateinit var veilederSomKanKassere: MockVeileder

    @BeforeAll
    fun beforeAll() {
        mockBruker = navMockService.createHappyBruker()
        veilederSomKanKassere = navMockService.createVeileder(ident = "Z999999", mockBruker = mockBruker)
    }

    @Test
    fun `skal kke kunne kassere aktivitet uten tilgang`() {
        val veilederUtenKasseringstilgang = navMockService.createVeileder(mockBruker = mockBruker)
        val aktivitet = aktivitetTestService.opprettAktivitet(
            mockBruker, AktivitetDtoTestBuilder.nyAktivitet(
                AktivitetTypeDTO.STILLING
            )
        )

        veilederUtenKasseringstilgang
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
        val versjonerFørKassering = aktivitetTestService.hentVersjoner(aktivitetId.toString(), mockBruker, veilederSomKanKassere)
        assertThat(versjonerFørKassering.size).isEqualTo(1)

        aktivitetTestService.kasserAktivitetskort(veilederSomKanKassere, opprettetAktivitet.id)

        val versjonerEtterKassering = aktivitetTestService.hentVersjoner(aktivitetId.toString(), mockBruker, veilederSomKanKassere)
        assertThat(versjonerEtterKassering.size).isEqualTo(2)
        val kassertVersjon = versjonerEtterKassering.find { it.transaksjonsType == KASSERT }!!
        assertThat(kassertVersjon.endretAv).isEqualTo(veilederSomKanKassere.navIdent)
        assertThat(kassertVersjon.endretAvType).isEqualTo(Innsender.NAV.toString())
        assertThat(kassertVersjon.endretDato).isCloseTo(Date(), 200)
    }

    @Test
    fun `skal overskrive bestemte felter på alle versjoner av aktiviteten når man kasserer`() {
        val opprettetAktivitet = aktivitetTestService.opprettAktivitet(mockBruker, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.STILLING))
        val aktivitetId = opprettetAktivitet.id

        aktivitetTestService.kasserAktivitetskort(veilederSomKanKassere, opprettetAktivitet.id)

        val versjoner = aktivitetTestService.hentVersjoner(aktivitetId.toString(), mockBruker, veilederSomKanKassere)
        versjoner.forEach { aktivitetVersjon ->
            assertThat(aktivitetVersjon.tittel).isEqualTo("Det var skrevet noe feil, og det er nå slettet")
            assertThat(aktivitetVersjon.avsluttetKommentar).isEqualTo("Kassert av NAV")
            assertThat(aktivitetVersjon.lenke).isEqualTo("Kassert av NAV")
            assertThat(aktivitetVersjon.beskrivelse).isEqualTo("Kassert av NAV")
        }
    }

    @Test
    fun `skal ikke overskrive endret-felter og statusfelt på tidligere versjoner av aktiviteten når man kasserer`() {
        val annenVeileder = navMockService.createVeileder(mockBruker = mockBruker)
        val opprettetAktivitet = aktivitetTestService.opprettAktivitet(mockBruker, annenVeileder, AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.STILLING))
        val aktivitetId = opprettetAktivitet.id

        aktivitetTestService.kasserAktivitetskort(veilederSomKanKassere, opprettetAktivitet.id)

        val versjoner = aktivitetTestService.hentVersjoner(aktivitetId.toString(), mockBruker, veilederSomKanKassere)
        val sorterteVersjoner = versjoner.sortedBy { it.endretDato }
        val førsteVersjon = sorterteVersjoner.first()
        assertThat(førsteVersjon.status).isEqualTo(AktivitetStatus.PLANLAGT)
        assertThat(førsteVersjon.endretAv).isEqualTo(annenVeileder.navIdent)
        val andreVersjon = sorterteVersjoner[1]
        assertThat(andreVersjon.status).isEqualTo(AktivitetStatus.AVBRUTT)
        assertThat(andreVersjon.endretAv).isEqualTo(veilederSomKanKassere.navIdent)
    }
}
