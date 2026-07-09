package no.nav.veilarbaktivitet.foresatteinnsynsrett

import no.nav.common.types.identer.Fnr
import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.poao.dab.spring_auth.TilgangsType
import no.nav.veilarbaktivitet.person.fodselsdato.PdlFodselsdatoClient
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException


@RestController
@RequestMapping("/api/innsynsrett")
class InnsynrettController(
    private val authService: IAuthService,
    private val pdlFodselsdatoClient: PdlFodselsdatoClient,
) {
    
    @PostMapping()
    fun foresatteHarInnsynsrett(@RequestBody input: InnsynsrettInboundDTO): InnsynsrettOutboundDTO {
        val fnr: String = if (authService.erEksternBruker()) {
            authService.getLoggedInnUser().get()
        } else {
            val fnr = input.fnr ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
            authService.sjekkTilgangTilPerson(Fnr(fnr), TilgangsType.LESE)
            fnr
        }

        return InnsynsrettOutboundDTO(foresatteHarInnsynsrett = pdlFodselsdatoClient.erUnder18(Fnr.ofValidFnr(fnr)))
    }

    data class InnsynsrettOutboundDTO(
        val foresatteHarInnsynsrett: Boolean
    )

    data class InnsynsrettInboundDTO(
        val fnr: String?
    )
}