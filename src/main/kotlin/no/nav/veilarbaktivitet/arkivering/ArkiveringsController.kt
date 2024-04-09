package no.nav.veilarbaktivitet.arkivering

import no.nav.common.auth.context.AuthContext
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.AuthContextHolderThreadLocal
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.poao.dab.spring_auth.AuthService
import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService
import no.nav.veilarbaktivitet.aktivitet.HistorikkService
import no.nav.veilarbaktivitet.arkivering.Arkiveringslogikk.aktiviteterOgDialogerOppdatertEtter
import no.nav.veilarbaktivitet.arkivering.Arkiveringslogikk.lagArkivPayload
import no.nav.veilarbaktivitet.config.OppfolgingsperiodeResource
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.person.EksternNavnService
import no.nav.veilarbaktivitet.person.Person.AktorId
import no.nav.veilarbaktivitet.person.UserInContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors


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
    private val authContextHolder: AuthContextHolder,
) {
    val executor: Executor = Executors.newFixedThreadPool(10)

    @GetMapping("/forhaandsvisning")
    @AuthorizeFnr(auditlogMessage = "lag forhåndsvisning av aktivitetsplan og dialog", resourceType = OppfolgingsperiodeResource::class, resourceIdParamName = "oppfolgingsperiodeId")
    fun forhaandsvisAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID): ForhaandsvisningOutboundDTO {
        val dataHentet = ZonedDateTime.now()
        val arkivPayload = hentArkivPayload(oppfølgingsperiodeId)

        val forhaandsvisningResultat = orkivarClient.hentPdfForForhaandsvisning(arkivPayload)

        return ForhaandsvisningOutboundDTO(
            forhaandsvisningResultat.pdf,
            dataHentet,
            forhaandsvisningResultat.sistJournalført
        )
    }

    @PostMapping("/journalfor")
    @AuthorizeFnr(auditlogMessage = "journalføre aktivitetsplan og dialog", resourceType = OppfolgingsperiodeResource::class, resourceIdParamName = "oppfolgingsperiodeId")
    fun arkiverAktivitetsplanOgDialog(@RequestParam("oppfolgingsperiodeId") oppfølgingsperiodeId: UUID, @RequestBody arkiverInboundDTO: ArkiverInboundDTO): JournalførtOutboundDTO {
        val arkivPayload = hentArkivPayload(oppfølgingsperiodeId, arkiverInboundDTO.forhaandsvisningOpprettet)
        val journalførtResult = orkivarClient.journalfor(arkivPayload)
        return JournalførtOutboundDTO(
            sistJournalført = journalførtResult.sistJournalført
        )
    }

    private fun hentArkivPayload(oppfølgingsperiodeId: UUID, forhaandsvisningTidspunkt: ZonedDateTime? = null): ArkivPayload {
        val fnr = userInContext.fnr.get()
        val aktorId = userInContext.aktorId
        val authContext = authContextHolder.context.get()

        val oppfølgingsperiodeFuture = asyncGet(authContext) { hentOppfølgingsperiode(aktorId, oppfølgingsperiodeId) }
        val dialogerFuture = asyncGet(authContext) { dialogClient.hentDialogerUtenKontorsperre(fnr) }
        val navnFuture = asyncGet(authContext) { navnService.hentNavn(fnr)}
        val sakFuture = asyncGet(authContext) { oppfølgingsperiodeService.hentSak(oppfølgingsperiodeId) }
        val målFuture = asyncGet(authContext) { oppfølgingsperiodeService.hentMål(fnr) }

        val aktiviteter = appService.hentAktiviteterUtenKontorsperre(fnr)
        val historikkForAktiviteter = historikkService.hentHistorikk(aktiviteter.map { it.id })
        val oppfølgingsperiode = oppfølgingsperiodeFuture.get()
        val dialoger = dialogerFuture.get()
        val navn = navnFuture.get()
        val sak = sakFuture.get() ?: throw RuntimeException("Kunne ikke hente sak for oppfølgingsperiode: $oppfølgingsperiodeId")
        val mål = målFuture.get()

        if (forhaandsvisningTidspunkt != null) {
            val oppdatertEtterForhaandsvisning = aktiviteterOgDialogerOppdatertEtter(forhaandsvisningTidspunkt, aktiviteter, dialoger)
            if (oppdatertEtterForhaandsvisning) throw ResponseStatusException(HttpStatus.CONFLICT)
        }

        // TODO: Clean up auth context in threads
        return lagArkivPayload(fnr, navn, oppfølgingsperiode, aktiviteter, dialoger, sak, mål, historikkForAktiviteter)
    }

    private fun hentOppfølgingsperiode(aktorId: AktorId, oppfølgingsperiodeId: UUID): OppfolgingPeriodeMinimalDTO {
        return oppfølgingsperiodeService.hentOppfolgingsperiode(aktorId, oppfølgingsperiodeId) ?: throw RuntimeException("Fant ingen oppfølgingsperiode for $oppfølgingsperiodeId")
    }

    private fun <T> asyncGet(authContext: AuthContext, supplier: () -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync (
            {
                val threadAuthContext = AuthContextHolderThreadLocal.instance()
                threadAuthContext.setContext(authContext)
                supplier.invoke()
            },
            executor
        )
    }

    data class ForhaandsvisningOutboundDTO(
        val pdf: ByteArray,
        val forhaandsvisningOpprettet: ZonedDateTime,
        val sistJournalført: LocalDateTime?
    )

    data class ArkiverInboundDTO(
        val forhaandsvisningOpprettet: ZonedDateTime
    )

    data class JournalførtOutboundDTO(
        val sistJournalført: LocalDateTime
    )
}
