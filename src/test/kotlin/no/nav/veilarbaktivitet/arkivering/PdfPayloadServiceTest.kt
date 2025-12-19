package no.nav.veilarbaktivitet.arkivering

import com.nimbusds.jwt.JWT
import no.nav.common.auth.context.AuthContext
import no.nav.common.auth.context.UserRole
import no.nav.common.types.identer.EnhetId
import no.nav.veilarbaktivitet.aktivitet.AktivitetId
import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.KvpUtvalgskriterieAlternativ.INKLUDER_KVP_AKTIVITETER
import no.nav.veilarbaktivitet.oppfolging.client.MålDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.person.EksternBruker
import no.nav.veilarbaktivitet.person.Navn
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.testutils.AktivitetDataTestBuilder
import no.nav.veilarbaktivitet.testutils.AktivitetTypeDataTestBuilder
import no.nav.veilarbaktivitet.util.DateUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.String

class PdfPayloadServiceTest {

    @Test
    fun `Skal aldri inkludere aktiviteter og dialoger med kontorsperre når man forhåndsviser`() {
        val pdfPayload = pdfPayloadServiceMedKontorsperretData()
            .lagPdfPayloadForForhåndsvisning(bruker, oppfølgingsperiodeId, defaultEnhetId)
        assertThat(pdfPayload.dialogtråder).isEmpty()
        assertThat(pdfPayload.aktiviteter).isEmpty()
    }

    @Test
    fun `Skal aldri inkludere aktiviteter og dialoger med kontorsperre når man journalfører`() {
        val pdfPayloadResult = pdfPayloadServiceMedKontorsperretData()
            .lagPdfPayloadForJournalføring(bruker, oppfølgingsperiodeId, defaultEnhetId, ZonedDateTime.now())
        assertThat(pdfPayloadResult.getOrThrow().dialogtråder).isEmpty()
        assertThat(pdfPayloadResult.getOrThrow().aktiviteter).isEmpty()
    }

    @Test
    fun `Skal ikke inkludere aktiviteter og dialoger med kontorsperre når man sender til bruker og filter ekskluderer KVP`() {
        val pdfPayloadResult = pdfPayloadServiceMedKontorsperretData()
            .lagPdfPayloadForSendTilBruker(bruker, oppfølgingsperiodeId, defaultEnhetId, null, tomtFilterUtenKvp,ZonedDateTime.now())
        assertThat(pdfPayloadResult.getOrThrow().dialogtråder).isEmpty()
        assertThat(pdfPayloadResult.getOrThrow().aktiviteter).isEmpty()
    }

    @Test
    fun `Returner feil hvis man forsøker å inkludere KVP-data når man sender til bruker`() {
        val inkluderKvpFilter = tomtFilterUtenKvp.copy(kvpUtvalgskriterie = ArkiveringsController.KvpUtvalgskriterie(INKLUDER_KVP_AKTIVITETER))
        val pdfPayloadResult = pdfPayloadServiceMedKontorsperretData().lagPdfPayloadForSendTilBruker(bruker, oppfølgingsperiodeId, defaultEnhetId, null, inkluderKvpFilter,ZonedDateTime.now())
        assertThat(pdfPayloadResult.isFailure).isTrue
    }

    @Test
    fun `Skal kunne inkludere aktiviteter og dialoger med kontorsperre når man lager payload for forhåndsvisning for utskrift`() {
        val inkluderKvpFilter = tomtFilterUtenKvp.copy(kvpUtvalgskriterie = ArkiveringsController.KvpUtvalgskriterie(INKLUDER_KVP_AKTIVITETER))
        val pdfPayload = pdfPayloadServiceMedKontorsperretData()
            .lagPdfPayloadForForhåndsvisningUtskrift(bruker, oppfølgingsperiodeId, defaultEnhetId, null, inkluderKvpFilter)
        assertThat(pdfPayload.dialogtråder).hasSize(1)
        assertThat(pdfPayload.aktiviteter).hasSize(1)
    }

    @Test
    fun `Skal ikke kunne inkludere aktiviteter og dialoger med kontorsperre hvis veileder ikke har tilgang`() {
        val inkluderKvpFilter = tomtFilterUtenKvp.copy(kvpUtvalgskriterie = ArkiveringsController.KvpUtvalgskriterie(INKLUDER_KVP_AKTIVITETER))
        val pdfPayloadService = pdfPayloadServiceMedKontorsperretData(harTilgangTilEnhet = false)
        val pdfPayload = pdfPayloadService
            .lagPdfPayloadForForhåndsvisningUtskrift(bruker, oppfølgingsperiodeId, defaultEnhetId, null, inkluderKvpFilter)
        assertThat(pdfPayload.dialogtråder).hasSize(0)
        assertThat(pdfPayload.aktiviteter).hasSize(0)
    }

    @Test
    fun `Returner feil hvis aktiviteter er oppdatert etter forhåndsvisningstidspunkt ved journalføring`() {
        val forhåndsvisningstidspunkt = ZonedDateTime.now().minusMinutes(5)
        val pdfPayloadService = pdfPayloadService(
            aktiviteter = listOf(nyAktivitet(sistOppdatert = forhåndsvisningstidspunkt.plusSeconds(1))),
        )
        val pdfPayloadResult = pdfPayloadService
            .lagPdfPayloadForJournalføring(bruker, oppfølgingsperiodeId, defaultEnhetId, forhåndsvisningstidspunkt)
        assertThat(pdfPayloadResult.isFailure).isTrue
    }

    @Test
    fun `Returner feil hvis dialoger er oppdatert etter forhåndsvisningstidspunkt ved journalføring`() {
        val forhåndsvisningstidspunkt = ZonedDateTime.now().minusMinutes(5)
        val pdfPayloadService = pdfPayloadService(
            dialoger = listOf(nyDialogtråd(sistOppdatert = forhåndsvisningstidspunkt.plusSeconds(1))),
        )
        val pdfPayloadResult = pdfPayloadService
            .lagPdfPayloadForJournalføring(bruker, oppfølgingsperiodeId, defaultEnhetId, forhåndsvisningstidspunkt)
        assertThat(pdfPayloadResult.isFailure).isTrue
    }

    @Test
    fun `Returner feil hvis aktiviteter er oppdatert etter forhåndsvisningstidspunkt ved send-til-bruker`() {
        val forhåndsvisningstidspunkt = ZonedDateTime.now().minusMinutes(5)
        val pdfPayloadService = pdfPayloadService(
            aktiviteter = listOf(nyAktivitet(sistOppdatert = forhåndsvisningstidspunkt.plusSeconds(1))),
        )
        val pdfPayloadResult = pdfPayloadService
            .lagPdfPayloadForSendTilBruker(bruker, oppfølgingsperiodeId, defaultEnhetId, null, tomtFilterUtenKvp,forhåndsvisningstidspunkt)
        assertThat(pdfPayloadResult.isFailure).isTrue
    }

    @Test
    fun `Returner feil hvis dialoger er oppdatert etter forhåndsvisningstidspunkt ved send-til-bruker`() {
        val forhåndsvisningstidspunkt = ZonedDateTime.now().minusMinutes(5)
        val pdfPayloadService = pdfPayloadService(
            dialoger = listOf(nyDialogtråd(sistOppdatert = forhåndsvisningstidspunkt.plusSeconds(1))),
        )
        val pdfPayloadResult = pdfPayloadService
            .lagPdfPayloadForSendTilBruker(bruker, oppfølgingsperiodeId, defaultEnhetId, null, tomtFilterUtenKvp,forhåndsvisningstidspunkt)
        assertThat(pdfPayloadResult.isFailure).isTrue
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

        val pdfPayloadResultJournalføring = pdfPayloadService.lagPdfPayloadForJournalføring(bruker, oppfølgingsperiodeId, defaultEnhetId, ZonedDateTime.now())
        assertThat(pdfPayloadResultJournalføring.getOrThrow().dialogtråder).isEmpty()
        assertThat(pdfPayloadResultJournalføring.getOrThrow().aktiviteter).isEmpty()

        val pdfPayloadForhåndsvisningUtskrift = pdfPayloadService.lagPdfPayloadForForhåndsvisningUtskrift(bruker, oppfølgingsperiodeId, defaultEnhetId, null, tomtFilterUtenKvp)
        assertThat(pdfPayloadForhåndsvisningUtskrift.dialogtråder).isEmpty()
        assertThat(pdfPayloadForhåndsvisningUtskrift.aktiviteter).isEmpty()

        val pdfPayloadSendTilBruker = pdfPayloadService.lagPdfPayloadForSendTilBruker(bruker, oppfølgingsperiodeId, defaultEnhetId, null, tomtFilterUtenKvp,
            ZonedDateTime.now())
        assertThat(pdfPayloadSendTilBruker.getOrThrow().dialogtråder).isEmpty()
        assertThat(pdfPayloadSendTilBruker.getOrThrow().aktiviteter).isEmpty()
    }

    fun pdfPayloadServiceMedKontorsperretData(harTilgangTilEnhet: Boolean = true): PdfPayloadService {
        return pdfPayloadService(
            dialoger = listOf(nyDialogtråd(kontorsperreEnhetId = "1234")),
            aktiviteter = listOf(nyAktivitet(kontorsperreEnhetId = "1234")),
            harTilgangTilEnhet = harTilgangTilEnhet
        )
    }
    private fun nyAktivitet(
        id: Long = 1,
        kontorsperreEnhetId: String? = null,
        oppfølgingsperiodeId: UUID = this@PdfPayloadServiceTest.oppfølgingsperiodeId,
        sistOppdatert: ZonedDateTime = ZonedDateTime.now().minusMonths(1),
    ) =
        AktivitetDataTestBuilder.nyAktivitet()
            .oppfolgingsperiodeId(oppfølgingsperiodeId)
            .id(id)
            .aktivitetType(AktivitetTypeData.IJOBB)
            .iJobbAktivitetData(AktivitetTypeDataTestBuilder.nyIJobbAktivitet())
            .kontorsperreEnhetId(kontorsperreEnhetId)
            .endretDato(DateUtils.zonedDateTimeToDate(sistOppdatert))
            .build()

    private fun nyDialogtråd(
        id: String = "1234",
        kontorsperreEnhetId: String? = null,
        aktivitetId: String? = null,
        oppfølgingsperiodeId: UUID = defaultOppfølgingsperiode.uuid,
        sistOppdatert: ZonedDateTime = ZonedDateTime.now().minusMonths(1),
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
            sisteDato = sistOppdatert,
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
