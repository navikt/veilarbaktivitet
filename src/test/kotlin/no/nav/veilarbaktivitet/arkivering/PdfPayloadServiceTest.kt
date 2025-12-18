package no.nav.veilarbaktivitet.arkivering

import com.nimbusds.jwt.JWT
import no.nav.common.auth.context.AuthContext
import no.nav.common.auth.context.UserRole
import no.nav.common.types.identer.EnhetId
import no.nav.veilarbaktivitet.aktivitet.AktivitetId
import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.oppfolging.client.MålDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.person.EksternBruker
import no.nav.veilarbaktivitet.person.Navn
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.String

class PdfPayloadServiceTest {

    @Test
    fun `Skal aldri inkludere aktiviteter og dialoger med kontorsperre når man forhåndsviser`() {
        val pdfPayloadService = pdfPayloadService(
            dialoger = listOf(nyDialogtråd(kontorsperreEnhetId = "1234")),
            aktiviteter = listOf(nyAktivitet(kontorsperreEnhetId = "1234"))
        )
        val pdfPayload = pdfPayloadService.lagPdfPayloadForForhåndsvisning(bruker, oppfølgingsperiodeId, defaultEnhetId)
        assertThat(pdfPayload.dialogtråder).isEmpty()
        assertThat(pdfPayload.aktiviteter).isEmpty()
    }

    @Test
    fun `Skal aldri inkludere aktiviteter og dialoger utenfor oppfølgingsperiode når man forhåndsviser`() {
        val annenOppfølgingsperiodeId = UUID.randomUUID()
        val pdfPayloadService = pdfPayloadService(
            dialoger = listOf(nyDialogtråd(oppfølgingsperiodeId = annenOppfølgingsperiodeId)),
            aktiviteter = listOf(nyAktivitet(oppfølgingsperiodeId = annenOppfølgingsperiodeId))
        )

        val pdfPayloadForhåndsvisning = pdfPayloadService.lagPdfPayloadForForhåndsvisning(bruker, oppfølgingsperiodeId, defaultEnhetId)
        assertThat(pdfPayloadForhåndsvisning.dialogtråder).isEmpty()
        assertThat(pdfPayloadForhåndsvisning.aktiviteter).isEmpty()
    }

    private fun nyAktivitet(
        id: Long = 1,
        kontorsperreEnhetId: String? = null,
        oppfølgingsperiodeId: UUID = this@PdfPayloadServiceTest.oppfølgingsperiodeId,
    ) =
        AktivitetDataTestBuilder.nyAktivitet().oppfolgingsperiodeId(oppfølgingsperiodeId).id(id).kontorsperreEnhetId(kontorsperreEnhetId).build()

    private fun nyDialogtråd(
        id: String = "1234",
        kontorsperreEnhetId: String? = null,
        aktivitetId: String? = null,
        oppfølgingsperiodeId: UUID = defaultOppfølgingsperiode.uuid
    ): DialogClient.DialogTråd {
        return DialogClient.DialogTråd(
            id = id,
            aktivitetId = aktivitetId,
            overskrift = "Overskrift",
            kontorsperreEnhetId = kontorsperreEnhetId,
            oppfolgingsperiodeId = oppfølgingsperiodeId,
            opprettetDato = ZonedDateTime.now().minusMonths(1),
            meldinger = listOf(),
            egenskaper = emptyList(),
            erLestAvBruker = false,
            lestAvBrukerTidspunkt = null,
            sisteDato = ZonedDateTime.now().minusMonths(1),
        )
    }

    private val defaultEnhetId = EnhetId.of("1234")
    private val oppfølgingsperiodeId = UUID.randomUUID()
    private val defaultOppfølgingsperiode = OppfolgingPeriodeMinimalDTO(
        oppfølgingsperiodeId, ZonedDateTime.now().minusMonths(2), null
    )

    private val bruker = EksternBruker(Person.Fnr.fnr("12345678901"), Person.AktorId("1000000000001"))

    private val defaultAuthContext = AuthContext(
        UserRole.INTERN,
        mock(JWT::class.java),
    )

    val tomtFilterUtenKvp = ArkiveringsController.Filter(
        inkluderHistorikk = true,
        kvpUtvalgskriterie = ArkiveringsController.KvpUtvalgskriterie(ArkiveringsController.KvpUtvalgskriterieAlternativ.EKSKLUDER_KVP_AKTIVITETER),
        inkluderDialoger = true,
        aktivitetAvtaltMedNavFilter = emptyList(),
        stillingsstatusFilter = emptyList(),
        arenaAktivitetStatusFilter = emptyList(),
        aktivitetTypeFilter = emptyList()
    )

    fun pdfPayloadService(
        dialoger: List<DialogClient.DialogTråd> = emptyList(),
        navn: Navn = Navn("Fornavn", "Mellomnavn", "Etternavn"),
        aktiviteter: List<AktivitetData> = emptyList(),
        oppfølgingsperiode: OppfolgingPeriodeMinimalDTO = defaultOppfølgingsperiode,
        mål: MålDTO = MålDTO("Mål"),
        historikk: Map<AktivitetId, Historikk> = emptyMap(),
        arenaAktiviteter: List<ArenaAktivitetDTO> = emptyList(),
        kontorNavn: String = "Nav Helsfyr",
        harTilgangTilEnhet: Boolean = true,
        authContext: AuthContext = defaultAuthContext
    ) = PdfPayloadService(
        hentDialoger = { dialoger },
        hentNavn = { navn },
        hentAktiviteter = { aktiviteter },
        hentOppfølgingsperiode = { _, _ -> oppfølgingsperiode },
        hentMål = { mål },
        hentHistorikk = { historikk },
        hentArenaAktiviteter = { arenaAktiviteter },
        hentKontorNavn = { kontorNavn },
        harTilgangTilEnhet = { harTilgangTilEnhet },
        getAuthContext = { authContext },
    )
}