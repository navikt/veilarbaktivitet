package no.nav.veilarbaktivitet.aktivitet

import no.nav.common.types.identer.EnhetId
import no.nav.veilarbaktivitet.aktivitet.domain.PlanlagtMote
import no.nav.veilarbaktivitet.person.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.Date

class PlanlagtMoteServiceTest {
    private val dao = mock<PlanlagteMoterDAO>()
    private val service = AktivitetService(mock(), mock(), mock(), mock(), dao)
    private val aktorId = Person.aktorId("1234567890123")
    private val fraDato = Date.from(Instant.parse("2026-01-01T12:00:00Z"))

    @Test
    fun `moter uten kontorsperre krever ikke enhetssjekk`() {
        val mote = PlanlagtMote(1L, fraDato)
        whenever(dao.hentPlanlagteMoter(eq(aktorId), any())).thenReturn(listOf(mote))

        val resultat = service.hentPlanlagteMoterPersonHarTilgangTil(aktorId) {
            error("Skal ikke sjekke enhetstilgang for et mote uten kontorsperre")
        }

        assertThat(resultat).contains(mote)
    }

    @Test
    fun `velger forste tilgjengelige mote med ett DAO-oppslag og en sjekk per enhet`() {
        val utenTilgang = EnhetId.of("1234")
        val medTilgang = EnhetId.of("5678")
        val skjult = PlanlagtMote(1L, fraDato, utenTilgang)
        val ogsaSkjult = PlanlagtMote(2L, Date(fraDato.time + 60_000), utenTilgang)
        val synlig = PlanlagtMote(3L, Date(fraDato.time + 120_000), medTilgang)
        val senere = PlanlagtMote(4L, Date(fraDato.time + 180_000))
        whenever(dao.hentPlanlagteMoter(eq(aktorId), any()))
            .thenReturn(listOf(skjult, ogsaSkjult, synlig, senere))
        val sjekkedeEnheter = mutableListOf<EnhetId>()

        val resultat = service.hentPlanlagteMoterPersonHarTilgangTil(aktorId) {
            sjekkedeEnheter.add(it)
            it == medTilgang
        }

        assertThat(resultat).contains(synlig)
        assertThat(sjekkedeEnheter).containsExactly(utenTilgang, medTilgang)
        verify(dao).hentPlanlagteMoter(eq(aktorId), any())
        verifyNoMoreInteractions(dao)
    }

    @Test
    fun `returnerer tomt nar ingen moter er synlige for person pga kontorsperre`() {
        val mote = PlanlagtMote(1L, fraDato, EnhetId.of("1234"))
        whenever(dao.hentPlanlagteMoter(eq(aktorId), any())).thenReturn(listOf(mote))

        val resultat = service.hentPlanlagteMoterPersonHarTilgangTil(aktorId) { false }

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `returnerer tomt nar bruker ikke har planlagte moter`() {
        whenever(dao.hentPlanlagteMoter(eq(aktorId), any())).thenReturn(emptyList())

        val resultat = service.hentPlanlagteMoterPersonHarTilgangTil(aktorId) {
            error("Skal ikke sjekke enhetstilgang nar det ikke finnes moter")
        }

        assertThat(resultat).isEmpty()
    }
}
