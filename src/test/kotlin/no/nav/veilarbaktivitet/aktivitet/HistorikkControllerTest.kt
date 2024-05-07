package no.nav.veilarbaktivitet.aktivitet

import no.nav.veilarbaktivitet.SpringBootTestBase
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetStatus
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetTypeDTO
import no.nav.veilarbaktivitet.person.Innsender
import no.nav.veilarbaktivitet.testutils.AktivitetDtoTestBuilder
import no.nav.veilarbaktivitet.util.DateUtils.dateToZonedDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.temporal.ChronoUnit

class HistorikkControllerTest: SpringBootTestBase() {

    @Test
    fun `Skal lage historikk for en aktivitet`() {
        val bruker = navMockService.createHappyBruker()
        val veileder = navMockService.createVeileder(bruker)
        val sisteOppfølgingsperiode = bruker.oppfolgingsperioder.maxBy { it.startTid }
        val aktivitet = AktivitetDtoTestBuilder.nyAktivitet(AktivitetTypeDTO.SAMTALEREFERAT)
            .toBuilder().oppfolgingsperiodeId(sisteOppfølgingsperiode.oppfolgingsperiodeId).build()
        val opprettetAktivitet = aktivitetTestService.opprettAktivitet(bruker, veileder, aktivitet)
        val oppdatertAktivitet = aktivitetTestService.oppdaterAktivitetStatus(bruker, veileder, opprettetAktivitet, AktivitetStatus.GJENNOMFORES)

        val historikk = veileder
            .createRequest(bruker)
            .get("http://localhost:$port/veilarbaktivitet/api/historikk/${opprettetAktivitet.id}")
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response()
            .`as`(AktivitetHistorikk::class.java)

        assertThat(historikk.endringer).hasSize(2)

        val førsteEndring = historikk.endringer.first()
        assertThat(førsteEndring.endretAvType).isEqualTo(Innsender.NAV)
        assertThat(førsteEndring.endretAv).isEqualTo(veileder.navIdent)
        assertThat(førsteEndring.tidspunkt).isCloseTo(dateToZonedDateTime(oppdatertAktivitet.endretDato), within(100, ChronoUnit.MILLIS))
        assertThat(førsteEndring.beskrivelseForVeileder).isEqualTo("${veileder.navIdent} flyttet aktiviteten fra Planlagt til Gjennomføres")
        assertThat(førsteEndring.beskrivelseForBruker).isEqualTo("NAV flyttet aktiviteten fra Planlagt til Gjennomføres")
        assertThat(førsteEndring.beskrivelseForArkiv).isEqualTo("NAV flyttet aktiviteten fra Planlagt til Gjennomføres")

        val andreEndring = historikk.endringer[1]
        assertThat(andreEndring.endretAvType).isEqualTo(Innsender.NAV)
        assertThat(andreEndring.endretAv).isEqualTo(veileder.navIdent)
        assertThat(andreEndring.tidspunkt).isCloseTo(dateToZonedDateTime(opprettetAktivitet.endretDato), within(1, ChronoUnit.MILLIS))
        assertThat(andreEndring.beskrivelseForVeileder).isEqualTo("${veileder.navIdent} opprettet aktiviteten")
        assertThat(andreEndring.beskrivelseForBruker).isEqualTo("NAV opprettet aktiviteten")
        assertThat(andreEndring.beskrivelseForArkiv).isEqualTo("NAV opprettet aktiviteten")
    }
}
