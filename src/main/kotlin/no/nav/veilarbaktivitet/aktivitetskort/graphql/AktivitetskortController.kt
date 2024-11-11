package no.nav.veilarbaktivitet.aktivitetskort.graphql

import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
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
import no.nav.veilarbaktivitet.config.ownerProviders.AktivitetOwnerProvider
import no.nav.veilarbaktivitet.person.Person
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
    val authService: IAuthService
) {

    @QueryMapping
    fun perioder(@Argument fnr: String): List<OppfolgingsPeriode> {
        val fnr = getContextUserIdent(fnr)
        val eksternBrukerId = Fnr.of(fnr.get())
        authService.sjekkTilgangTilPerson(eksternBrukerId, TilgangsType.LESE)
        val aktiviteter = getAktiviteter(fnr)
            .groupBy { it.oppfolgingsperiodeId }
            .toList()
            .map { OppfolgingsPeriode(it.first, it.second) }
        return aktiviteter
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

    @SchemaMapping(typeName="AktivitetDTO", field="historikk")
    fun getHistorikk(aktivitet: AktivitetDTO): Historikk {
        val aktivitetId = aktivitet.id.toLong()
        return historikkService.hentHistorikk(listOf(aktivitetId))[aktivitetId]!!
    }

    private fun getContextUserIdent(fnr: String): Person {
        return when {
            authService.erEksternBruker() -> Person.fnr(authService.getLoggedInnUser().get())
            fnr.isNotBlank() -> Person.fnr(fnr)
            else -> throw throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
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
