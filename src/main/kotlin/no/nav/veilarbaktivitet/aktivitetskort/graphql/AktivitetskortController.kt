package no.nav.veilarbaktivitet.aktivitetskort.graphql

import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.veilarbaktivitet.aktivitet.AktivitetAppService
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService
import no.nav.veilarbaktivitet.person.Person
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller


@Controller
class AktivitetskortController(
    val appService: AktivitetAppService,
    val migreringService: MigreringService,
    val authService: IAuthService
) {

    @QueryMapping
    fun perioder(@Argument fnr: String): List<OppfolgingsPeriode> {
        val fnr = getContextUserIdent(fnr)
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
            .map { a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker) }
        return migreringService.visMigrerteArenaAktiviteterHvisToggleAktiv(aktiviter)
    }

    private fun getContextUserIdent(fnr: String): Person {
        return when {
            authService.erEksternBruker() -> Person.fnr(authService.getLoggedInnUser().get())
            fnr.isNotBlank() -> Person.fnr(fnr)
            else -> throw RuntimeException()
        }
    }
}
