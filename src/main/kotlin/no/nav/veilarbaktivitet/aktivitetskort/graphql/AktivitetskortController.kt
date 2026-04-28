package no.nav.veilarbaktivitet.aktivitetskort.graphql

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.EnhetId
import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.poao.dab.spring_auth.TilgangsType
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService
import no.nav.veilarbaktivitet.aktivitet.AktivitetService
import no.nav.veilarbaktivitet.aktivitet.Historikk
import no.nav.veilarbaktivitet.aktivitet.HistorikkService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService
import no.nav.veilarbaktivitet.arena.ArenaService
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.config.ownerProviders.AktivitetOwnerProvider
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.person.Person
import org.springframework.beans.factory.annotation.Value
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.server.ResponseStatusException

@Controller
class AktivitetskortController(
    val appService: AktivitetAppService,
    val migreringService: MigreringService,
    val historikkService: HistorikkService,
    val ownerProvider: AktivitetOwnerProvider,
    val aktivitetService: AktivitetService,
    val aktivitetAppService: AktivitetAppService,
    val oppfolgingsperiodeService: OppfolgingsperiodeService,
    val aktorOppslagClient: AktorOppslagClient,
    val authService: IAuthService,
    val arenaService: ArenaService,
) {
    @Value("\${app.env.poaoAdminIdenter}")
    var godkjenteAdminIdenterInput: String? = null
    val godkjenteAdminIndenter by lazy {
        godkjenteAdminIdenterInput
            ?.split(",".toRegex())
            ?.dropLastWhile { it.isEmpty() }
            ?: emptyList()
    }

    @QueryMapping
    fun perioder(@Argument fnr: String): List<OppfolgingsPeriode> {
        val eksternBrukerId = getContextUserIdent(fnr)
        authService.sjekkTilgangTilPerson(eksternBrukerId.otherFnr(), TilgangsType.LESE)
        val aktorId = aktorOppslagClient.hentAktorId(eksternBrukerId.otherFnr())
        val oppfolgingsPerioder = oppfolgingsperiodeService.hentOppfolgingsPerioder(Person.AktorId(aktorId.get()))
        val aktiviteter = getAktiviteter(eksternBrukerId)
        return oppfolgingsPerioder
            .map { periode ->
                val periodeAktiviteter = aktiviteter.filter { it.oppfolgingsperiodeId == periode.oppfolgingsperiodeId }
                OppfolgingsPeriode(periode.oppfolgingsperiodeId, periodeAktiviteter, periode.startTid, periode.sluttTid)
            }
            .sortedByDescending { it.start }
    }

    private fun getAktiviteter(userFnr: Person): List<AktivitetDTO> {
        val erEksternBruker: Boolean = authService.erEksternBruker()
        val aktiviter = appService
            .hentAktiviteterForIdent(userFnr)
            .let { filtrerKontorsperret(it) }
            .map { a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker) }
        return migreringService.visMigrerteArenaAktiviteterHvisToggleAktiv(aktiviter)
    }

    @QueryMapping
    fun aktivitet(@Argument aktivitetId: Long): AktivitetDTO {
        val erEksternBruker = authService.erEksternBruker()
        val eksternBrukerId = ownerProvider.getOwner(aktivitetId.toString())
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No owner found for aktivitetId")
        authService.sjekkTilgangTilPerson(eksternBrukerId, TilgangsType.LESE)
        return aktivitetAppService.hentAktivitet(aktivitetId)
            .let { AktivitetDTOMapper.mapTilAktivitetDTO(it, erEksternBruker) }
    }

    @QueryMapping
    fun eier(@Argument aktivitetId: Long): Eier {
        val eksternBrukerId = ownerProvider.getOwner(aktivitetId.toString())
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No owner found for aktivitetId")
        authService.sjekkTilgangTilPerson(eksternBrukerId, TilgangsType.LESE)
        return Eier(eksternBrukerId.get())
    }

    @QueryMapping
    fun tiltaksaktiviteter(@Argument fnr: String): List<ArenaAktivitetDTO> {
        val adminIdent = authService.getInnloggetVeilederIdent()
        if (!godkjenteAdminIndenter.contains(adminIdent.get())) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        val eksternBrukerId = getContextUserIdent(fnr)
        return arenaService.hentAktiviteterRaw(eksternBrukerId)
    }

    @SchemaMapping(typeName="AktivitetDTO", field="historikk")
    fun getHistorikk(aktivitet: AktivitetDTO): Historikk {
        val aktivitetId = aktivitet.id.toLong()
        return historikkService.hentHistorikk(listOf(aktivitetId))[aktivitetId]!!
    }

    private fun getContextUserIdent(fnr: String): Person.Fnr {
        return when {
            authService.erEksternBruker() -> Person.fnr(authService.getLoggedInnUser().get())
            fnr.isNotBlank() -> Person.fnr(fnr)
            else -> throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        }
    }

    private fun filtrerKontorsperret(aktiviteter: List<AktivitetData>): List<AktivitetData> {
        // Ikke spør om kontortilgang på nytt for hver aktivitet
        val enheterMedTilgang = mutableMapOf<EnhetId, Boolean>()
        return aktiviteter.filter { aktivitet ->
            val enhetId = aktivitet.kontorsperreEnhetId?.let(EnhetId::of)
            when {
                enhetId == null -> true
                enheterMedTilgang[enhetId] == true -> true
                else -> {
                    authService.harTilgangTilEnhet(enhetId)
                        .also { enheterMedTilgang[enhetId] = it }
                }
            }
        }
    }
}
