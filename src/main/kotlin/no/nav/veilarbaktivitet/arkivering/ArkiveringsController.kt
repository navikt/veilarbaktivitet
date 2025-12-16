package no.nav.veilarbaktivitet.arkivering

import no.nav.common.types.identer.EnhetId
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.veilarbaktivitet.arena.model.ArenaStatusEtikettDTO
import no.nav.veilarbaktivitet.arkivering.mapper.ArkiveringspayloadMapper.mapTilArkivPayload
import no.nav.veilarbaktivitet.arkivering.mapper.toArkivEtikett
import no.nav.veilarbaktivitet.config.OppfolgingsperiodeResource
import no.nav.veilarbaktivitet.manuell_status.v2.ManuellStatusV2Client
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.person.UserInContext
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
    private val oppfølgingsperiodeService: OppfolgingsperiodeService,
    private val manuellStatusClient: ManuellStatusV2Client,
    private val arkiveringService: ArkiveringService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val Tema_AktivitetsplanMedDialog = "AKT"
    }

    @PostMapping("/forhaandsvisning")
    @AuthorizeFnr(auditlogMessage = "lag forhåndsvisning av aktivitetsplan og dialog", resourceType = OppfolgingsperiodeResource::class, resourceIdParamName = "oppfolgingsperiodeId")
    fun forhaandsvisAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID, @RequestBody forhaandsvisningInboundDto: ForhaandsvisningInboundDTO): ForhaandsvisningOutboundDTO {
        val dataHentet = ZonedDateTime.now()
        val pdfPayload = arkiveringService.lagPdfPayloadForForhåndsvisning(oppfølgingsperiodeId, EnhetId.of(forhaandsvisningInboundDto.journalførendeEnhetId))

        val timedForhaandsvisningResultat = measureTimedValue {
            orkivarClient.hentPdfForForhaandsvisning(pdfPayload)
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
    fun forhaandsvisAktivitetsplanOgDialogUtskrift(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID, @RequestBody forhaandsvisningSendTilBrukerInboundDto: ForhaandsvisningSendTilBrukerInboundDto): ForhaandsvisningOutboundDTO {
        val dataHentet = ZonedDateTime.now()
        val journalførendeEnhetId = forhaandsvisningSendTilBrukerInboundDto.journalførendeEnhetId
        val pdfPayload = arkiveringService.lagPdfPayloadForForhåndsvisningUtskrift(
            oppfølgingsperiodeId = oppfølgingsperiodeId,
            journalførendeEnhetId = if (journalførendeEnhetId != null && journalførendeEnhetId.isNotBlank()) EnhetId.of(journalførendeEnhetId) else null,
            tekstTilBruker = forhaandsvisningSendTilBrukerInboundDto.tekstTilBruker,
            filter = forhaandsvisningSendTilBrukerInboundDto.filter
        )
        val timedForhaandsvisningResultat = measureTimedValue {
            orkivarClient.hentPdfForForhaandsvisningSendTilBruker(pdfPayload)
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
    fun arkiverAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID, @RequestBody journalførInboundDTO: JournalførInboundDTO): JournalførtOutboundDTO {
        val pdfPayloadResult = arkiveringService.lagPdfPayloadForJournalføring(oppfølgingsperiodeId, EnhetId.of(journalførInboundDTO.journalførendeEnhetId), journalførInboundDTO.forhaandsvisningOpprettet)
        val pdfPayload = pdfPayloadResult.getOrElse {
            val statusCode = (it as? ResponseStatusException)?.statusCode ?: HttpStatus.INTERNAL_SERVER_ERROR
            throw ResponseStatusException(statusCode)
        }
        val sak = oppfølgingsperiodeService.hentSak(oppfølgingsperiodeId) ?: throw RuntimeException("Kunne ikke hente sak for oppfølgingsperiode: $oppfølgingsperiodeId")
        val arkivPayload = mapTilArkivPayload(
            pdfPayload = pdfPayload,
            sakDTO = sak,
            journalførendeEnhetId = EnhetId.of(journalførInboundDTO.journalførendeEnhetId),
            tema = Tema_AktivitetsplanMedDialog,
        )
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
        val pdfPayloadResult = arkiveringService.lagPdfPayloadForUtskrift(
            oppfølgingsperiodeId = oppfølgingsperiodeId,
            journalførendeEnhetId = EnhetId.of(sendTilBrukerInboundDTO.journalførendeEnhetId),
            tekstTilBruker = sendTilBrukerInboundDTO.tekstTilBruker,
            filter = sendTilBrukerInboundDTO.filter,
            ikkeOppdatertEtter = sendTilBrukerInboundDTO.forhaandsvisningOpprettet
        )
        val pdfPayload = pdfPayloadResult.getOrElse {
            val statusCode = (it as? ResponseStatusException)?.statusCode ?: HttpStatus.INTERNAL_SERVER_ERROR
            throw ResponseStatusException(statusCode)
        }

        val sak = oppfølgingsperiodeService.hentSak(oppfølgingsperiodeId) ?: throw RuntimeException("Kunne ikke hente sak for oppfølgingsperiode: $oppfølgingsperiodeId")
        val arkivPayload = mapTilArkivPayload(pdfPayload, sak, EnhetId.of(sendTilBrukerInboundDTO.journalførendeEnhetId), Tema_AktivitetsplanMedDialog)
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

    data class ForhaandsvisningInboundDTO(
        val journalførendeEnhetId: String
    )

    data class ForhaandsvisningOutboundDTO(
        val pdf: ByteArray,
        val forhaandsvisningOpprettet: ZonedDateTime,
        val sistJournalført: LocalDateTime?
    )

    data class JournalførInboundDTO(
        val forhaandsvisningOpprettet: ZonedDateTime,
        val journalførendeEnhetId: String
    )

    data class JournalførtOutboundDTO(
        val sistJournalført: LocalDateTime
    )

    data class SendTilBrukerInboundDTO(
        val forhaandsvisningOpprettet: ZonedDateTime,
        val journalførendeEnhetId: String,
        val tekstTilBruker: String,
        val filter: Filter
    )

    data class ForhaandsvisningSendTilBrukerInboundDto(
        val tekstTilBruker: String,
        val journalførendeEnhetId: String?,
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
