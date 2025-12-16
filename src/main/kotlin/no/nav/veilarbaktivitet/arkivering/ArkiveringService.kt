package no.nav.veilarbaktivitet.arkivering

import kotlinx.coroutines.*
import no.nav.common.auth.context.AuthContext
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.types.identer.EnhetId
import no.nav.poao.dab.spring_auth.AuthService
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService
import no.nav.veilarbaktivitet.aktivitet.HistorikkService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData.SAMTALEREFERAT
import no.nav.veilarbaktivitet.arena.ArenaService
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.*
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.KvpUtvalgskriterieAlternativ.EKSKLUDER_KVP_AKTIVITETER
import no.nav.veilarbaktivitet.arkivering.mapper.ArkiveringspayloadMapper.mapTilPdfPayload
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2Client
import no.nav.veilarbaktivitet.norg2.Norg2Client
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.person.EksternNavnService
import no.nav.veilarbaktivitet.person.UserInContext
import no.nav.veilarbaktivitet.util.EnheterTilgangCache
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.measureTimedValue

class ArkiveringService(
    private val userInContext: UserInContext,
    private val dialogClient: DialogClient,
    private val navnService: EksternNavnService,
    private val appService: AktivitetAppService,
    private val oppfølgingsperiodeService: OppfolgingsperiodeService,
    private val historikkService: HistorikkService,
    private val arenaService: ArenaService,
    private val manuellStatusClient: ManuellStatusV2Client,
    private val authContextHolder: AuthContextHolder,
    private val norg2Client: Norg2Client,
    private val authService: AuthService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lagPdfPayloadForForhåndsvisning(oppfølgingsperiodeId: UUID, journalførendeEnhetId: EnhetId): PdfPayload {
        if(!authService.erInternBruker()) throw RuntimeException("Skal kun brukes av interne brukere")
        val tomtFilterUtenKvp = defaultFilter()
        val ufiltrertArkiveringsdata = hentArkiveringsData(oppfølgingsperiodeId = oppfølgingsperiodeId, journalførendeEnhetId = journalførendeEnhetId)
        val filtrertArkiveringsdata = filtrerArkiveringsData(ufiltrertArkiveringsdata, tomtFilterUtenKvp)
        return mapTilPdfPayload(arkiveringsData = filtrertArkiveringsdata, filter = null)
    }

    private fun defaultFilter() = Filter(
        inkluderHistorikk = true,
        kvpUtvalgskriterie = KvpUtvalgskriterie(EKSKLUDER_KVP_AKTIVITETER),
        inkluderDialoger = true,
        aktivitetAvtaltMedNavFilter = emptyList(),
        stillingsstatusFilter = emptyList(),
        arenaAktivitetStatusFilter = emptyList(),
        aktivitetTypeFilter = emptyList()
    )

    private fun hentArkiveringsData(
        oppfølgingsperiodeId: UUID,
        journalførendeEnhetId: EnhetId?,
        tekstTilBruker: String? = null,
    ): ArkiveringsData {
        val timedArkiveringsdata = measureTimedValue {
            val fnr = userInContext.fnr.get()
            val aktorId = userInContext.aktorId
            val authContext = authContextHolder.context.get()
            val enheterTilgangCache = EnheterTilgangCache(authService::harTilgangTilEnhet)
            val journalførendeEnhetNavn = journalførendeEnhetId?.let { norg2Client.hentKontorNavn(it.get()) } ?: ""

            fun <T> CoroutineScope.hentDataAsync(hentData: () -> T): Deferred<T> =
                hentDataAsyncMedAuthContext(authContext, hentData)

            runBlocking(Dispatchers.IO) {
                val aktiviteterDeferred = hentDataAsync {
                    appService.hentAktiviteterForIdent(fnr)
                        .asSequence()
                        .filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }
                        .filter { it.kontorsperreEnhetId == null ||  enheterTilgangCache.harTilgang(it.kontorsperreEnhetId) }
                        .filterNot { it.aktivitetType == SAMTALEREFERAT && it.moteData?.isReferatPublisert == false }
                        .sortedByDescending { it.endretDato }
                        .toList()
                }
                val dialogerIPerioden = hentDataAsync {
                    dialogClient.hentDialoger(fnr)
                        .filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }
                        .filter { it.kontorsperreEnhetId == null || enheterTilgangCache.harTilgang(it.kontorsperreEnhetId) }
                }
                val arenaAktiviteter = hentDataAsync {
                    arenaService.hentArenaAktiviteter(fnr).filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }
                }
                val oppfølgingsperiode = hentDataAsync {
                    oppfølgingsperiodeService.hentOppfolgingsperiode(aktorId, oppfølgingsperiodeId) ?:
                    throw RuntimeException("Fant ingen oppfølgingsperiode for $oppfølgingsperiodeId")
                }
                val navn = hentDataAsync { navnService.hentNavn(fnr) }
                val mål = hentDataAsync { oppfølgingsperiodeService.hentMål(fnr) }

                val aktiviteter = aktiviteterDeferred.await()
                val historikk = hentDataAsync { historikkService.hentHistorikk(aktiviteter.map { it.id }) }

                ArkiveringsData(
                    fnr = fnr,
                    navn = navn.await(),
                    tekstTilBruker = tekstTilBruker,
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
}