package no.nav.veilarbaktivitet.admin

import no.nav.common.auth.context.AuthContextHolder
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeService
import no.nav.veilarbaktivitet.person.Person.fnr
import no.nav.veilarbaktivitet.person.PersonService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val oppfolgingsperiodeService: OppfolgingsperiodeService,
    private val personService: PersonService,
    private val poaoTilgangClient: PoaoTilgangClient,
    private val authContextHolder: AuthContextHolder,
    @Value("\${app.adminGroups:}")
    private val adminGroups: List<UUID> = emptyList()
) {


    @PutMapping("/avsluttoppfolgingsperiode/{oppfolgingsperiodeUuid}")
    @ResponseStatus(value = HttpStatus.OK)
    fun avsluttOppfolgingsperiode(@PathVariable("oppfolgingsperiodeUuid") oppfolgingsperiodeUuid: String, @RequestParam fnr: String) {
        val azureId = authContextHolder.requireOid()
        poaoTilgangClient.hentAdGrupper(azureId).get()?.find { adminGroups.contains(it.id) } ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Veileder er ikke i admingroup")
        val aktorId = personService.getAktorIdForPersonBruker(fnr(fnr)).get()
        val sluttDato = oppfolgingsperiodeService.hentOppfolgingsperiode(aktorId, UUID.fromString(oppfolgingsperiodeUuid))?.sluttDato()
            ?: throw NoSuchElementException("Finner ikke oppfolgingsperiode / sluttdato")
        oppfolgingsperiodeService.avsluttOppfolgingsperiode(UUID.fromString(oppfolgingsperiodeUuid), sluttDato)
    }
}