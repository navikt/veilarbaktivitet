package no.nav.veilarbaktivitet.arkivering

import kotlinx.coroutines.*
import no.nav.common.auth.context.AuthContext
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.poao.dab.spring_auth.TilgangsType
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService
import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.HistorikkService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetTypeData.SAMTALEREFERAT
import no.nav.veilarbaktivitet.arena.ArenaService
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arkivering.mapper.ArkiveringspayloadMapper.mapTilArkivPayload
import no.nav.veilarbaktivitet.arkivering.mapper.ArkiveringspayloadMapper.mapTilForhåndsvisningsPayload
import no.nav.veilarbaktivitet.config.OppfolgingsperiodeResource
import no.nav.veilarbaktivitet.oppfolging.client.MålDTO
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.person.EksternNavnService
import no.nav.veilarbaktivitet.person.Navn
import no.nav.veilarbaktivitet.person.Person.Fnr
import no.nav.veilarbaktivitet.person.UserInContext
import no.nav.veilarbaktivitet.util.DateUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
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
    private val authContextHolder: AuthContextHolder,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/forhaandsvisning")
    @AuthorizeFnr(auditlogMessage = "lag forhåndsvisning av aktivitetsplan og dialog", resourceType = OppfolgingsperiodeResource::class, resourceIdParamName = "oppfolgingsperiodeId")
    fun forhaandsvisAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID): ForhaandsvisningOutboundDTO {
        val dataHentet = ZonedDateTime.now()
        val arkiveringsdata = hentArkiveringsData(oppfølgingsperiodeId)
        val forhåndsvisningPayload = mapTilForhåndsvisningsPayload(arkiveringsdata)

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

    @PostMapping("/journalfor")
    @AuthorizeFnr(auditlogMessage = "journalføre aktivitetsplan og dialog", resourceType = OppfolgingsperiodeResource::class, resourceIdParamName = "oppfolgingsperiodeId", tilgangsType = TilgangsType.SKRIVE)
    fun arkiverAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID, @RequestBody arkiverInboundDTO: ArkiverInboundDTO): JournalførtOutboundDTO {
        val arkiveringsdata = hentArkiveringsData(oppfølgingsperiodeId)

        val oppdatertEtterForhaandsvisning = aktiviteterOgDialogerOppdatertEtter(arkiverInboundDTO.forhaandsvisningOpprettet, arkiveringsdata.aktiviteter, arkiveringsdata.dialoger)
        if (oppdatertEtterForhaandsvisning) throw ResponseStatusException(HttpStatus.CONFLICT)

        val sak = oppfølgingsperiodeService.hentSak(oppfølgingsperiodeId) ?: throw RuntimeException("Kunne ikke hente sak for oppfølgingsperiode: $oppfølgingsperiodeId")
        val arkivPayload = mapTilArkivPayload(arkiveringsdata, sak, arkiverInboundDTO.journalforendeEnhet)

        val timedJournalførtResultat = measureTimedValue {
            orkivarClient.journalfor(arkivPayload)
        }
        logger.info("Journalføring av PDF tok ${timedJournalførtResultat.duration.inWholeMilliseconds} ms")
        val journalførtResult = timedJournalførtResultat.value

        return JournalførtOutboundDTO(
            sistJournalført = journalførtResult.sistJournalført
        )
    }

    private fun hentArkiveringsData(oppfølgingsperiodeId: UUID): ArkiveringsData {
        val timedArkiveringsdata = measureTimedValue {
            val fnr = userInContext.fnr.get()
            val aktorId = userInContext.aktorId
            val authContext = authContextHolder.context.get()

            fun <T> CoroutineScope.hentDataAsync(hentData: () -> T): Deferred<T> =
                hentDataAsyncMedAuthContext(authContext, hentData)

            runBlocking(Dispatchers.IO) {
                val aktiviteterDeferred = hentDataAsync {
                    appService.hentAktiviteterUtenKontorsperre(fnr)
                        .asSequence()
                        .filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }
                        .filterNot { it.aktivitetType == SAMTALEREFERAT && it.moteData?.isReferatPublisert == false }
                        .toList()
                }
                val dialogerIPerioden = hentDataAsync {
                    dialogClient.hentDialogerUtenKontorsperre(fnr)
                        .filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }
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
}
