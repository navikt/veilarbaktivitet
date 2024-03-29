package no.nav.veilarbaktivitet.arkivering

import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.arkivering.Arkiveringslogikk.aktiviteterOgDialogerOppdatertEtter
import no.nav.veilarbaktivitet.oppfolging.client.MålDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.oppfolging.client.SakDTO
import no.nav.veilarbaktivitet.person.Navn
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

class ArkiveringslogikkTest {

    private val dummyFnr = Person.fnr("987654321")
    private val dummyNavn = Navn("Fornavn", null, "Etternavn")

    @Test
    fun `OppfølgingsperiodeSluttdato skal være null hvis oppfølgingsperiode er aktiv`() {
        val oppfølgingsperiode = OppfolgingPeriodeMinimalDTO(UUID.randomUUID(), ZonedDateTime.now().minusDays(10), null)
        val annenOppfølgingsperiodeId = UUID.randomUUID()
        val aktivitetUtenforOppfølgingsperiode = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.IJOBB)
            .toBuilder().oppfolgingsperiodeId(annenOppfølgingsperiodeId).build()

        val arkivPayload = Arkiveringslogikk.lagArkivPayload(
            dummyFnr,
            dummyNavn,
            oppfølgingsperiode,
            aktiviteter = listOf(aktivitetUtenforOppfølgingsperiode),
            dialoger = emptyList(),
            sakDTO = SakDTO(oppfølgingsperiode.uuid, 1000L, "ARBEIDSOPPFOLGING"),
            mål = MålDTO("Å få jobb"),
            historikkForAktiviteter = mapOf()
        )

        assertThat(arkivPayload.metadata.oppfølgingsperiodeSlutt).isNull()
    }

    @Test
    fun `Payload skal ikke inkludere aktiviteter utenfor oppfølgingsperiode`() {
        val oppfølgingsperiode = OppfolgingPeriodeMinimalDTO(UUID.randomUUID(), ZonedDateTime.now().minusDays(10), ZonedDateTime.now())
        val annenOppfølgingsperiodeId = UUID.randomUUID()
        val aktivitetUtenforOppfølgingsperiode = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.IJOBB)
            .toBuilder().oppfolgingsperiodeId(annenOppfølgingsperiodeId).build()

        val arkivPayload = Arkiveringslogikk.lagArkivPayload(
            dummyFnr,
            dummyNavn,
            oppfølgingsperiode,
            aktiviteter = listOf(aktivitetUtenforOppfølgingsperiode),
            dialoger = emptyList(),
            sakDTO = SakDTO(oppfølgingsperiode.uuid, 1000L, "ARBEIDSOPPFOLGING"),
            mål = MålDTO("Å få jobb"),
            historikkForAktiviteter = mapOf()
        )

        assertThat(arkivPayload.aktiviteter).isEmpty()
    }

    @Test
    fun `Payload skal ikke inkludere dialoger utenfor oppfølgingsperiode`() {
        val oppfølgingsperiode = OppfolgingPeriodeMinimalDTO(UUID.randomUUID(), ZonedDateTime.now().minusDays(10), ZonedDateTime.now())
        val annenOppfølgingsperiodeId = UUID.randomUUID()
        val dialog = DialogClient.DialogTråd(
            id = "Id",
            aktivitetId = null,
            overskrift = "Overskrift",
            oppfolgingsperiodeId = annenOppfølgingsperiodeId,
            meldinger = emptyList(),
            egenskaper = emptyList()
        )

        val arkivPayload = Arkiveringslogikk.lagArkivPayload(
            dummyFnr,
            dummyNavn,
            oppfølgingsperiode,
            aktiviteter = emptyList(),
            dialoger = listOf(dialog),
            sakDTO = SakDTO(oppfølgingsperiode.uuid, 1000L, "ARBEIDSOPPFOLGING"),
            mål = MålDTO("Å få jobb"),
            historikkForAktiviteter = mapOf()
        )

        assertThat(arkivPayload.aktiviteter).isEmpty()
    }

    @Test
    fun `Skal returnere true når en aktivitet er endret etter tidspunktet for forhåndsvisning`() {
        val forhåndsvistTidspunkt = ZonedDateTime.now().minusSeconds(1)
        val aktivitetEndret = Date.from(Instant.now())
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.IJOBB).toBuilder()
            .endretDato(aktivitetEndret).build()

        val resultat = aktiviteterOgDialogerOppdatertEtter(forhåndsvistTidspunkt, listOf(aktivitet), emptyList())

        assertThat(resultat).isTrue()
    }

    @Test
    fun `Skal returnere false når en aktivitet ikke er endret etter tidspunktet for forhåndsvisning`() {
        val forhåndsvistTidspunkt = ZonedDateTime.now()
        val aktivitetEndret = Date.from(Instant.now().minusSeconds(1))
        val aktivitet = AktivitetDataTestBuilder.nyAktivitet(AktivitetTypeData.IJOBB).toBuilder()
            .endretDato(aktivitetEndret).build()

        val resultat = aktiviteterOgDialogerOppdatertEtter(forhåndsvistTidspunkt, listOf(aktivitet), emptyList())

        assertThat(resultat).isFalse()
    }
}