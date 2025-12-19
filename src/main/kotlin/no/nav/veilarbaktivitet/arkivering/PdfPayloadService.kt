package no.nav.veilarbaktivitet.arkivering

import kotlinx.coroutines.*
import no.nav.common.auth.context.AuthContext
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.types.identer.EnhetId
import no.nav.veilarbaktivitet.aktivitet.AktivitetId
import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData.SAMTALEREFERAT
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.Filter
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.KvpUtvalgskriterie
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.KvpUtvalgskriterieAlternativ.EKSKLUDER_KVP_AKTIVITETER
import no.nav.veilarbaktivitet.arkivering.mapper.ArkiveringspayloadMapper.mapTilPdfPayload
import no.nav.veilarbaktivitet.oppfolging.client.MålDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.person.EksternBruker
import no.nav.veilarbaktivitet.person.Navn
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.util.DateUtils
import no.nav.veilarbaktivitet.util.EnheterTilgangCache
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.ZonedDateTime
import java.util.*
import kotlin.time.measureTimedValue

class PdfPayloadService(
    private val hentDialoger: (Person.Fnr) -> List<DialogClient.DialogTråd>,
    private val hentNavn: (Person.Fnr) -> Navn,
    private val hentAktiviteter: (Person.Fnr) -> List<AktivitetData>,
    private val hentOppfølgingsperiode: (Person.AktorId, UUID) -> OppfolgingPeriodeMinimalDTO?,
    private val hentMål: (Person.Fnr) -> MålDTO,
    private val hentHistorikk: (List<AktivitetId>) -> Map<AktivitetId, Historikk>,
    private val hentArenaAktiviteter: (Person.Fnr) -> List<ArenaAktivitetDTO>,
    private val hentKontorNavn: (String) -> String,
    private val harTilgangTilEnhet: (enhet: EnhetId) -> Boolean,
    private val getAuthContext: () -> AuthContext,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lagPdfPayloadForForhåndsvisning(bruker: EksternBruker, oppfølgingsperiodeId: UUID, journalførendeEnhetId: EnhetId): PdfPayload {
        val arkiveringsdata = hentArkiveringsData(bruker, oppfølgingsperiodeId, journalførendeEnhetId, tomtFilterUtenKvp)
        return mapTilPdfPayload(arkiveringsData = arkiveringsdata,  tekstTilBruker = null, filter = tomtFilterUtenKvp)
    }

    fun lagPdfPayloadForJournalføring(bruker: EksternBruker, oppfølgingsperiodeId: UUID, journalførendeEnhetId: EnhetId, forhåndsvisningsTidspunkt: ZonedDateTime): Result<PdfPayload> {
        val arkiveringsdata = hentArkiveringsData(bruker, oppfølgingsperiodeId, journalførendeEnhetId, tomtFilterUtenKvp)
        val oppdatertEtterForhaandsvisning = aktiviteterOgDialogerOppdatertEtter(forhåndsvisningsTidspunkt, arkiveringsdata)
        if (oppdatertEtterForhaandsvisning) {
            return Result.failure(ResponseStatusException(HttpStatus.CONFLICT))
        }
        return Result.success(mapTilPdfPayload(arkiveringsData = arkiveringsdata,  tekstTilBruker = null, filter = tomtFilterUtenKvp))
    }

    fun lagPdfPayloadForForhåndsvisningUtskrift(bruker: EksternBruker, oppfølgingsperiodeId: UUID, journalførendeEnhetId: EnhetId?, tekstTilBruker: String?, filter: Filter): PdfPayload {
        val arkiveringsdata = hentArkiveringsData(bruker, oppfølgingsperiodeId, journalførendeEnhetId, filter)
        return mapTilPdfPayload(arkiveringsData = arkiveringsdata, filter = filter, tekstTilBruker = tekstTilBruker)
    }

    fun lagPdfPayloadForSendTilBruker(bruker: EksternBruker, oppfølgingsperiodeId: UUID, journalførendeEnhetId: EnhetId, tekstTilBruker: String?, filter: Filter, forhåndsvisningsTidspunkt: ZonedDateTime): Result<PdfPayload> {
        val inkludererKvpAktiviteter = filter.kvpUtvalgskriterie.alternativ != EKSKLUDER_KVP_AKTIVITETER
        if (inkludererKvpAktiviteter) {
            return Result.failure(ResponseStatusException(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS))
        }
        val arkiveringsdata = hentArkiveringsData(bruker, oppfølgingsperiodeId, journalførendeEnhetId, filter)
        val oppdatertEtterForhaandsvisning = aktiviteterOgDialogerOppdatertEtter(forhåndsvisningsTidspunkt, arkiveringsdata)
        if (oppdatertEtterForhaandsvisning) {
            return Result.failure(ResponseStatusException(HttpStatus.CONFLICT))
        }
        return Result.success(mapTilPdfPayload(arkiveringsData = arkiveringsdata, tekstTilBruker = tekstTilBruker, filter = filter))
    }

    private fun hentArkiveringsData(bruker: EksternBruker, oppfølgingsperiodeId: UUID, journalførendeEnhetId: EnhetId?, filter: Filter): ArkiveringsData {
        val ufiltrertArkiveringsdata = hentArkiveringsData(bruker = bruker, oppfølgingsperiodeId = oppfølgingsperiodeId, journalførendeEnhetId = journalførendeEnhetId)
        return filtrerArkiveringsData(ufiltrertArkiveringsdata, filter)
    }

    private val tomtFilterUtenKvp = Filter(
        inkluderHistorikk = true,
        kvpUtvalgskriterie = KvpUtvalgskriterie(EKSKLUDER_KVP_AKTIVITETER),
        inkluderDialoger = true,
        aktivitetAvtaltMedNavFilter = emptyList(),
        stillingsstatusFilter = emptyList(),
        arenaAktivitetStatusFilter = emptyList(),
        aktivitetTypeFilter = emptyList()
    )

    private fun hentArkiveringsData(
        bruker: EksternBruker,
        oppfølgingsperiodeId: UUID,
        journalførendeEnhetId: EnhetId?,
    ): ArkiveringsData {
        val timedArkiveringsdata = measureTimedValue {
            val authContext = getAuthContext()
            val enheterTilgangCache = EnheterTilgangCache(harTilgangTilEnhet)
            val journalførendeEnhetNavn = journalførendeEnhetId?.let { hentKontorNavn(it.get()) } ?: ""

            fun <T> CoroutineScope.hentDataAsync(hentData: () -> T): Deferred<T> =
                hentDataAsyncMedAuthContext(authContext, hentData)

            runBlocking(Dispatchers.IO) {
                val aktiviteterDeferred = hentDataAsync {
                    hentAktiviteter(bruker.fnr)
                        .asSequence()
                        .filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }
                        .filter { it.kontorsperreEnhetId == null ||  enheterTilgangCache.harTilgang(it.kontorsperreEnhetId) }
                        .filterNot { it.aktivitetType == SAMTALEREFERAT && it.moteData?.isReferatPublisert == false }
                        .sortedByDescending { it.endretDato }
                        .toList()
                }
                val dialogerIPerioden = hentDataAsync {
                    hentDialoger(bruker.fnr)
                        .filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }
                        .filter { it.kontorsperreEnhetId == null || enheterTilgangCache.harTilgang(it.kontorsperreEnhetId) }
                }
                val arenaAktiviteter = hentDataAsync {
                    hentArenaAktiviteter(bruker.fnr).filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }
                }
                val oppfølgingsperiode = hentDataAsync {
                    hentOppfølgingsperiode(bruker.aktorId, oppfølgingsperiodeId) ?:
                    throw RuntimeException("Fant ingen oppfølgingsperiode for $oppfølgingsperiodeId")
                }
                val navn = hentDataAsync { hentNavn(bruker.fnr) }
                val mål = hentDataAsync { hentMål(bruker.fnr) }

                val aktiviteter = aktiviteterDeferred.await()
                val historikk = hentDataAsync { hentHistorikk(aktiviteter.map { it.id }) }

                ArkiveringsData(
                    fnr = bruker.fnr,
                    navn = navn.await(),
                    journalførendeEnhetNavn = journalførendeEnhetNavn,
                    oppfølgingsperiode = oppfølgingsperiode.await(),
                    aktiviteter = aktiviteter,
                    dialoger = dialogerIPerioden.await(),
                    mål = mål.await(),
                    historikkForAktiviteter = historikk.await(),
                    arenaAktiviteter = arenaAktiviteter.await()
                )
            }
        }
        logger.info("Henting av data tok ${timedArkiveringsdata.duration.inWholeMilliseconds} ms")
        return timedArkiveringsdata.value
    }

    private fun <T> CoroutineScope.hentDataAsyncMedAuthContext(authContext: AuthContext, hentData: () -> T): Deferred<T> {
        return async {
            val threadAuthContext = AuthContextHolderThreadLocal.instance()
            threadAuthContext.setContext(authContext)
            hentData()
        }
    }

    private fun aktiviteterOgDialogerOppdatertEtter(
        tidspunkt: ZonedDateTime,
        arkiveringsdata: ArkiveringsData
    ): Boolean {
        val aktiviteterTidspunkt = arkiveringsdata.aktiviteter.map { DateUtils.dateToZonedDateTime(it.endretDato) }
        val dialogerTidspunkt = arkiveringsdata.dialoger.mapNotNull { it.sisteDato }
        val sistOppdatert =
            (aktiviteterTidspunkt + dialogerTidspunkt).maxOrNull() ?: ZonedDateTime.now().minusYears(100)
        return sistOppdatert > tidspunkt
    }
}

data class ArkiveringsData(
    val fnr: Person.Fnr,
    val navn: Navn,
    val journalførendeEnhetNavn: String,
    val oppfølgingsperiode: OppfolgingPeriodeMinimalDTO,
    val aktiviteter: List<AktivitetData>,
    val dialoger: List<DialogClient.DialogTråd>,
    val mål: MålDTO,
    val historikkForAktiviteter: Map<Long, Historikk>,
    val arenaAktiviteter: List<ArenaAktivitetDTO>
)

