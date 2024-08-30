package no.nav.veilarbaktivitet.arkivering

import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
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
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

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
    private val arenaService: ArenaService
) {
    @GetMapping("/forhaandsvisning")
    @AuthorizeFnr(auditlogMessage = "lag forhåndsvisning av aktivitetsplan og dialog", resourceType = OppfolgingsperiodeResource::class, resourceIdParamName = "oppfolgingsperiodeId")
    fun forhaandsvisAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID): ForhaandsvisningOutboundDTO {
        val dataHentet = ZonedDateTime.now()
        val arkiveringsdata = hentArkiveringsData(userInContext.fnr.get(), oppfølgingsperiodeId)
        val forhåndsvisningPayload = mapTilForhåndsvisningsPayload(arkiveringsdata)

        val forhaandsvisningResultat = orkivarClient.hentPdfForForhaandsvisning(forhåndsvisningPayload)

        return ForhaandsvisningOutboundDTO(
            forhaandsvisningResultat.pdf,
            dataHentet,
            forhaandsvisningResultat.sistJournalført
        )
    }

    @PostMapping("/journalfor")
    @AuthorizeFnr(auditlogMessage = "journalføre aktivitetsplan og dialog", resourceType = OppfolgingsperiodeResource::class, resourceIdParamName = "oppfolgingsperiodeId")
    fun arkiverAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID, @RequestBody arkiverInboundDTO: ArkiverInboundDTO): JournalførtOutboundDTO {
        val arkiveringsdata = hentArkiveringsData(userInContext.fnr.get(), oppfølgingsperiodeId)

        val oppdatertEtterForhaandsvisning = aktiviteterOgDialogerOppdatertEtter(arkiverInboundDTO.forhaandsvisningOpprettet, arkiveringsdata.aktiviteter, arkiveringsdata.dialoger)
        if (oppdatertEtterForhaandsvisning) throw ResponseStatusException(HttpStatus.CONFLICT)

        val sak = oppfølgingsperiodeService.hentSak(oppfølgingsperiodeId) ?: throw RuntimeException("Kunne ikke hente sak for oppfølgingsperiode: $oppfølgingsperiodeId")
        val arkivPayload = mapTilArkivPayload(arkiveringsdata, sak, arkiverInboundDTO.journalforendeEnhet)

        val journalførtResult = orkivarClient.journalfor(arkivPayload)
        return JournalførtOutboundDTO(
            sistJournalført = journalførtResult.sistJournalført
        )
    }

    private fun hentArkiveringsData(fnr: Fnr, oppfølgingsperiodeId: UUID): ArkiveringsData {
        val aktiviteter = appService.hentAktiviteterUtenKontorsperre(fnr)
            .asSequence()
            .filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }
            .filterNot { it.aktivitetType == SAMTALEREFERAT && it.moteData?.isReferatPublisert == false }
            .toList()
        val dialogerIPerioden = dialogClient.hentDialogerUtenKontorsperre(fnr).filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }
        val arenaAktiviteter = arenaService.hentArenaAktiviteter(fnr).filter { it.oppfolgingsperiodeId == oppfølgingsperiodeId }

        return ArkiveringsData(
            fnr = fnr,
            navn = navnService.hentNavn(fnr),
            oppfølgingsperiode = oppfølgingsperiodeService.hentOppfolgingsperiode(userInContext.aktorId, oppfølgingsperiodeId) ?: throw RuntimeException("Fant ingen oppfølgingsperiode for $oppfølgingsperiodeId"),
            aktiviteter = aktiviteter,
            dialoger = dialogerIPerioden,
            mål = oppfølgingsperiodeService.hentMål(fnr),
            historikkForAktiviteter = historikkService.hentHistorikk(aktiviteter.map { it.id }),
            arenaAktiviteter = arenaAktiviteter
        )
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
