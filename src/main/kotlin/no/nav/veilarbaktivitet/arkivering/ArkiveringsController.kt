package no.nav.veilarbaktivitet.arkivering

import kotlinx.coroutines.*
import no.nav.common.auth.context.AuthContext
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.poao.dab.spring_auth.AuthService
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService
import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.HistorikkService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData.SAMTALEREFERAT
import no.nav.veilarbaktivitet.arena.ArenaService
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaStatusEtikettDTO
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.KvpUtvalgskriterieAlternativ.EKSKLUDER_KVP_AKTIVITETER
import no.nav.veilarbaktivitet.arkivering.ArkiveringsController.KvpUtvalgskriterieAlternativ.INKLUDER_KVP_AKTIVITETER
import no.nav.veilarbaktivitet.arkivering.mapper.ArkiveringspayloadMapper.mapTilArkivPayload
import no.nav.veilarbaktivitet.arkivering.mapper.ArkiveringspayloadMapper.mapTilForhåndsvisningsPayload
import no.nav.veilarbaktivitet.arkivering.mapper.toArkivEtikett
import no.nav.veilarbaktivitet.config.OppfolgingsperiodeResource
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2Client
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV3ClientImpl
import no.nav.veilarbaktivitet.oppfolging.client.MålDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.person.EksternNavnService
import no.nav.veilarbaktivitet.person.Navn
import no.nav.veilarbaktivitet.person.Person.Fnr
import no.nav.veilarbaktivitet.person.UserInContext
import no.nav.veilarbaktivitet.util.DateUtils
import no.nav.veilarbaktivitet.util.EnheterTilgangCache
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.time.measureTimedValue

@RestController
@RequestMapping("/api/arkivering")
class ArkiveringsController(
    private val userInContext: UserInContext,
    private val orkivarClient: OrkivarClient,
    private val dialogClient: DialogClient,
    private val navnService: EksternNavnService,
    private val appService: AktivitetAppService,
    private val oppfølgingsperiodeService: OppfolgingsperiodeService,
    private val historikkService: HistorikkService,
    private val arenaService: ArenaService,
    private val manuellStatusClient: ManuellStatusV2Client,
    private val authContextHolder: AuthContextHolder,
    private val authService: AuthService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val Tema_AktivitetsplanMedDialog = "AKT"
    }

    @GetMapping("/forhaandsvisning")
    @AuthorizeFnr(auditlogMessage = "lag forhåndsvisning av aktivitetsplan og dialog", resourceType = OppfolgingsperiodeResource::class, resourceIdParamName = "oppfolgingsperiodeId")
    fun forhaandsvisAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID): ForhaandsvisningOutboundDTO {
        val dataHentet = ZonedDateTime.now()
        val arkiveringsdata = hentArkiveringsData(oppfølgingsperiodeId)

        val forhåndsvisningPayload = mapTilForhåndsvisningsPayload(arkiveringsdata, null)

        val timedForhaandsvisningResultat = measureTimedValue {
            orkivarClient.hentPdfForForhaandsvisning(forhåndsvisningPayload)
        }
        logger.info("Henting av PDF tok ${timedForhaandsvisningResultat.duration.inWholeMilliseconds} ms")
        val forhaandsvisningResultat = timedForhaandsvisningResultat.value

        return ForhaandsvisningOutboundDTO(
            forhaandsvisningResultat.pdf,
            dataHentet,
            forhaandsvisningResultat.sistJournalført
        )
    }

    @PostMapping("/forhaandsvisning-send-til-bruker")
    @AuthorizeFnr(auditlogMessage = "lag forhåndsvisning av aktivitetsplan og dialog", resourceType = OppfolgingsperiodeResource::class, resourceIdParamName = "oppfolgingsperiodeId")
    fun forhaandsvisAktivitetsplanOgDialogSendTilBruker(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID, @RequestBody forhaandsvisningInboundDTO: ForhaandsvisningInboundDTO): ForhaandsvisningOutboundDTO {
        val dataHentet = ZonedDateTime.now()
        val valgtKvpAlternativ = forhaandsvisningInboundDTO.filter.kvpUtvalgskriterie.alternativ
        val hentKvpAktiviteter = valgtKvpAlternativ in listOf(
            INKLUDER_KVP_AKTIVITETER,
            KvpUtvalgskriterieAlternativ.KUN_KVP_AKTIVITETER
        )
        val filter = forhaandsvisningInboundDTO.filter
        val arkiveringsdata = hentArkiveringsData(oppfølgingsperiodeId, forhaandsvisningInboundDTO.tekstTilBruker,hentKvpAktiviteter)
        val filtrertArkiveringsdata = filtrerArkiveringsData(arkiveringsdata, filter)
        val forhåndsvisningPayload = mapTilForhåndsvisningsPayload(filtrertArkiveringsdata, filter)

        val timedForhaandsvisningResultat = measureTimedValue {
            orkivarClient.hentPdfForForhaandsvisningSendTilBruker(forhåndsvisningPayload)
        }
        logger.info("Henting av PDF tok ${timedForhaandsvisningResultat.duration.inWholeMilliseconds} ms")
        val forhaandsvisningResultat = timedForhaandsvisningResultat.value

        return ForhaandsvisningOutboundDTO(
            forhaandsvisningResultat.pdf,
            dataHentet,
            forhaandsvisningResultat.sistJournalført
        )
    }

    @PostMapping("/journalfor")
    @AuthorizeFnr(auditlogMessage = "journalføre aktivitetsplan og dialog", resourceType = OppfolgingsperiodeResource::class, resourceIdParamName = "oppfolgingsperiodeId")
    fun arkiverAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID, @RequestBody arkiverInboundDTO: ArkiverInboundDTO): JournalførtOutboundDTO {
        val arkiveringsdata = hentArkiveringsData(oppfølgingsperiodeId)

        val oppdatertEtterForhaandsvisning = aktiviteterOgDialogerOppdatertEtter(arkiverInboundDTO.forhaandsvisningOpprettet, arkiveringsdata.aktiviteter, arkiveringsdata.dialoger)
        if (oppdatertEtterForhaandsvisning) throw ResponseStatusException(HttpStatus.CONFLICT)

        val sak = oppfølgingsperiodeService.hentSak(oppfølgingsperiodeId) ?: throw RuntimeException("Kunne ikke hente sak for oppfølgingsperiode: $oppfølgingsperiodeId")
        val arkivPayload = mapTilArkivPayload(arkiveringsdata, sak, arkiverInboundDTO.journalforendeEnhet, Tema_AktivitetsplanMedDialog, null)

        val timedJournalførtResultat = measureTimedValue {
            orkivarClient.journalfor(arkivPayload)
        }
        logger.info("Journalføring av PDF tok ${timedJournalførtResultat.duration.inWholeMilliseconds} ms")
        val journalførtResult = timedJournalførtResultat.value

        return JournalførtOutboundDTO(
            sistJournalført = journalførtResult.sistJournalført
        )
    }

    @PostMapping("/send-til-bruker")
    @AuthorizeFnr(auditlogMessage = "Send aktivitetsplan til bruker", resourceType = OppfolgingsperiodeResource::class, resourceIdParamName = "oppfolgingsperiodeId")
    fun sendAktivitetsplanTilBruker(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID, @RequestBody sendTilBrukerInboundDTO: SendTilBrukerInboundDTO): ResponseEntity<Unit> {
        val inkludererKvpAktiviteter = sendTilBrukerInboundDTO.filter.kvpUtvalgskriterie.alternativ != EKSKLUDER_KVP_AKTIVITETER
        if (inkludererKvpAktiviteter) {
            throw ResponseStatusException(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS)
        }

        val filtrertArkiveringsdata = filtrerArkiveringsData(
            hentArkiveringsData(oppfølgingsperiodeId, sendTilBrukerInboundDTO.tekstTilBruker),
            sendTilBrukerInboundDTO.filter
        )

        val oppdatertEtterForhaandsvisning = aktiviteterOgDialogerOppdatertEtter(sendTilBrukerInboundDTO.forhaandsvisningOpprettet, filtrertArkiveringsdata.aktiviteter, filtrertArkiveringsdata.dialoger)
        if (oppdatertEtterForhaandsvisning) throw ResponseStatusException(HttpStatus.CONFLICT)

        val sak = oppfølgingsperiodeService.hentSak(oppfølgingsperiodeId) ?: throw RuntimeException("Kunne ikke hente sak for oppfølgingsperiode: $oppfølgingsperiodeId")
        val arkivPayload = mapTilArkivPayload(filtrertArkiveringsdata, sak, sendTilBrukerInboundDTO.journalforendeEnhet, Tema_AktivitetsplanMedDialog, sendTilBrukerInboundDTO.filter)
        val manuellStatus = manuellStatusClient.get(userInContext.aktorId).getOrNull()
        val erManuell = manuellStatus?.isErUnderManuellOppfolging ?: false

        val timedJournalførtResultat = measureTimedValue {
            orkivarClient.sendTilBruker(SendTilBrukerPayload(arkivPayload, erManuell))
        }
        logger.info("Sending av PDF til bruker tok ${timedJournalførtResultat.duration.inWholeMilliseconds} ms")

        return when (timedJournalførtResultat.value) {
            is OrkivarClient.SendTilBrukerSuccess -> ResponseEntity.status(204).build()
            is OrkivarClient.SendTilBrukerFail -> ResponseEntity.status(500).build()
        }
    }

    private fun hentArkiveringsData(oppfølgingsperiodeId: UUID, tekstTilBruker: String? = null, inkluderDataIKvpPeriode: Boolean = false): ArkiveringsData {
        val timedArkiveringsdata = measureTimedValue {
            val fnr = userInContext.fnr.get()
            val aktorId = userInContext.aktorId
            val authContext = authContextHolder.context.get()
            val enheterTilgangCache = EnheterTilgangCache(authService::harTilgangTilEnhet)

            fun <T> CoroutineScope.hentDataAsync(hentData: () -> T): Deferred<T> =
                hentDataAsyncMedAuthContext(authContext, hentData)

            runBlocking(Dispatchers.IO) {
                val aktiviteterDeferred = hentDataAsync {
                    val aktiviteter =
                        if (inkluderDataIKvpPeriode) appService.hentAktiviteterForIdent(fnr)
                        else appService.hentAktiviteterUtenKontorsperre(fnr) //TODO: Ta bort til slutt

                    aktiviteter
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
        aktiviteter: List<AktivitetData>,
        dialoger: List<DialogClient.DialogTråd>
    ): Boolean {
        val aktiviteterTidspunkt = aktiviteter.map { DateUtils.dateToZonedDateTime(it.endretDato) }
        val dialogerTidspunkt = dialoger.map { it.meldinger }.flatten().map { it.sendt }
        val sistOppdatert =
            (aktiviteterTidspunkt + dialogerTidspunkt).maxOrNull() ?: ZonedDateTime.now().minusYears(100)
        return sistOppdatert > tidspunkt
    }

    data class ArkiveringsData(
        val fnr: Fnr,
        val navn: Navn,
        val tekstTilBruker: String?,
        val oppfølgingsperiode: OppfolgingPeriodeMinimalDTO,
        val aktiviteter: List<AktivitetData>,
        val dialoger: List<DialogClient.DialogTråd>,
        val mål: MålDTO,
        val historikkForAktiviteter: Map<Long, Historikk>,
        val arenaAktiviteter: List<ArenaAktivitetDTO>
    )

    data class ForhaandsvisningOutboundDTO(
        val pdf: ByteArray,
        val forhaandsvisningOpprettet: ZonedDateTime,
        val sistJournalført: LocalDateTime?
    )

    data class ArkiverInboundDTO(
        val forhaandsvisningOpprettet: ZonedDateTime,
        val journalforendeEnhet: String
    )

    data class JournalførtOutboundDTO(
        val sistJournalført: LocalDateTime
    )

    data class SendTilBrukerInboundDTO(
        val forhaandsvisningOpprettet: ZonedDateTime,
        val journalforendeEnhet: String,
        val tekstTilBruker: String,
        val filter: Filter
    )

    data class ForhaandsvisningInboundDTO(
        val tekstTilBruker: String,
        val filter: Filter,
    )

    data class Filter(
        val inkluderHistorikk: Boolean,
        val kvpUtvalgskriterie: KvpUtvalgskriterie,
        val inkluderDialoger: Boolean,
        val aktivitetAvtaltMedNavFilter: List<AvtaltMedNavFilter>,
        val stillingsstatusFilter: List<SøknadsstatusFilter>,
        val arenaAktivitetStatusFilter: List<ArenaStatusEtikettDTO>,
        val aktivitetTypeFilter: List<AktivitetTypeFilter>,
    ) {
        fun mapTilBrukteFiltre(): Map<String, List<String>> {
            return mapOf(
                "Avtalt med Nav" to aktivitetAvtaltMedNavFilter.map { it.tekst },
                "Stillingsstatus" to stillingsstatusFilter.map { it.tekst },
                "Status for Arena-aktivitet" to arenaAktivitetStatusFilter.map { it.toArkivEtikett().tekst },
                "Aktivitetstype" to aktivitetTypeFilter.map { it.tekst}
            ).filter { it.value.isNotEmpty() }
        }
    }

    data class KvpUtvalgskriterie(
        val alternativ: KvpUtvalgskriterieAlternativ,
        val start: ZonedDateTime? = null,
        val slutt: ZonedDateTime? = null
    )

    enum class KvpUtvalgskriterieAlternativ {
        EKSKLUDER_KVP_AKTIVITETER,
        INKLUDER_KVP_AKTIVITETER,
        KUN_KVP_AKTIVITETER
    }

    enum class AvtaltMedNavFilter(val tekst: String) {
        AVTALT_MED_NAV("Avtalt med Nav"),
        IKKE_AVTALT_MED_NAV("Ikke avtalt med Nav"),
    }

    enum class SøknadsstatusFilter(val tekst: String) {
        AVSLAG("Avslag"),
        CV_DELT("CV delt"),
        IKKE_FATT_JOBBEN("Ikke fått jobben"),
        INGEN_VALGT("Ingen valgt"),
        INNKALT_TIL_INTERVJU("Innkalt til intervju"),
        JOBBTILBUD("Jobbtilbud"),
        SKAL_PAA_INTERVJU("Skal på intervju"),
        SOKNAD_SENDT("Søknad sendt"),
        VENTER("Venter på svar"),
        FATT_JOBBEN("Fått jobben"),
    }

    enum class AktivitetTypeFilter(val tekst: String) {
        ARENA_TILTAK("Tiltak gjennom Nav"),
        BEHANDLING("Behandling"),
        EGEN("Jobbrettet egenaktivitet"),
        GRUPPEAKTIVITET("Gruppeaktivitet"),
        IJOBB("Jobb jeg har nå"),
        MOTE("Møte med Nav"),
        SAMTALEREFERAT("Samtalereferat"),
        SOKEAVTALE("Jobbsøking"),
        STILLING("Stilling"),
        STILLING_FRA_NAV("Stilling fra Nav"),
        TILTAKSAKTIVITET("Tiltak gjennom Nav"),
        UTDANNINGSAKTIVITET("Utdanning"),
        MIDLERTIDIG_LONNSTILSKUDD("Midlertidig lønnstilskudd"),
        VARIG_LONNSTILSKUDD("Varig lønnstilskudd"),
        ARBEIDSTRENING("Arbeidstrening"),
        VARIG_TILRETTELAGT_ARBEID_I_ORDINAER_VIRKSOMHET("Varig tilrettelagt arbeid i ordinær virksomhet"),
        MENTOR("Mentor"),
        REKRUTTERINGSTREFF("Rekrutteringstreff"),
        ENKELAMO("Arbeidsmarkedsopplæring (enkeltplass)"),
        ENKFAGYRKE("Fag- og yrkesopplæring (enkeltplass)"),
        HOYEREUTD("Høyere utdanning")
    }
}
