package no.nav.veilarbaktivitet.arena

import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.Fnr
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.poao.dab.spring_auth.TilgangsType
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO
import no.nav.veilarbaktivitet.config.ForhaandsorienteringResource
import no.nav.veilarbaktivitet.config.OppfolgingsperiodeResource
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.UserInContext
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Transactional
@RestController
@Slf4j
@RequestMapping("/api/arena")
open class ArenaController(
    private val userInContext: UserInContext,
    private val authService: IAuthService,
    private val arenaService: ArenaService,
) {

    @PutMapping("/{oppfolgingsperiodeId}/forhaandsorientering")
    @AuthorizeFnr(auditlogMessage = "Opprett forhåndsorientering", resourceIdParamName = "oppfolgingsperiodeId", resourceType = OppfolgingsperiodeResource::class, tilgangsType = TilgangsType.SKRIVE)
    open fun opprettFHO(
        @RequestBody forhaandsorientering: ForhaandsorienteringDTO?,
        @RequestParam arenaaktivitetId: ArenaId,
        @RequestAttribute(name="fnr") fnr: Fnr
    ): ArenaAktivitetDTO {
        if (!authService.erInternBruker()) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Må være internbruker")
        getInputFeilmelding(forhaandsorientering, arenaaktivitetId)
            .ifPresent { feilmelding: String? -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, feilmelding) }
        val ident = authService.getInnloggetVeilederIdent()
        return arenaService.opprettFHO(arenaaktivitetId, Person.fnr(fnr.get()), forhaandsorientering!!, ident.get())
    }

    @GetMapping("/tiltak-raw")
    @AuthorizeFnr
    open fun hentAlleArenaAktiviteterRaw(): List<ArenaAktivitetDTO> {
        val fnr = userInContext.getFnr()
            .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "Må være på en bruker") }
        return arenaService.hentAktiviteterRaw(fnr)
    }

    data class FnrDto (val fnr: String?)

    @PostMapping("/tiltak")
    open fun postHentArenaAktiviteter(@RequestBody fnrDto: FnrDto) : List<ArenaAktivitetDTO> {
        val fnr: Fnr = if (authService.erEksternBruker()) {
            authService.getLoggedInnUser() as? Fnr ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Klarte ikke å hente ut fnr fra token")
        } else {
            if (fnrDto.fnr == null) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fnr kan ikke være null")
            Fnr.of(fnrDto.fnr)
        }
        authService.sjekkTilgangTilPerson(fnr, TilgangsType.LESE)
        return arenaService.hentArenaAktiviteter(Person.fnr(fnr.get()))
    }

    @PutMapping("/forhaandsorientering/lest")
    @AuthorizeFnr(auditlogMessage = "leste forhåndsorientering", resourceType = ForhaandsorienteringResource::class, resourceIdParamName = "aktivitetId", tilgangsType = TilgangsType.SKRIVE)
    open fun lest(@RequestParam aktivitetId: ArenaId): ArenaAktivitetDTO {
        if (!authService.erEksternBruker()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Bare eksterne-brukere kan lese FHO")
        val fnr = authService.getLoggedInnUser() as? Fnr ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Fant ikke innlogget ekstern-bruker")
        return arenaService.markerSomLest(fnr, aktivitetId)
    }

    open fun getInputFeilmelding(
        forhaandsorientering: ForhaandsorienteringDTO?,
        arenaaktivitetId: ArenaId?
    ): Optional<String> {
        if (arenaaktivitetId?.id() == null || arenaaktivitetId.id().isBlank()) {
            return Optional.of("arenaaktivitetId kan ikke være null eller tom")
        }
        if (forhaandsorientering == null) {
            return Optional.of("forhaandsorientering kan ikke være null")
        }
        if (forhaandsorientering.type == null) {
            return Optional.of("forhaandsorientering.type kan ikke være null")
        }
        return if (forhaandsorientering.tekst == null || forhaandsorientering.tekst.isEmpty()) {
            Optional.of("forhaandsorientering.tekst kan ikke være null eller tom")
        } else Optional.empty()
    }
}
