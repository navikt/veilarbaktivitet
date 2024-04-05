package no.nav.veilarbaktivitet.arena

import lombok.extern.slf4j.Slf4j
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingWithAktivitetStatus
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetTypeDTO
import no.nav.veilarbaktivitet.arena.model.ArenaId
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO
import no.nav.veilarbaktivitet.oppfolging.periode.Oppfolgingsperiode
import no.nav.veilarbaktivitet.oppfolging.periode.OppfolgingsperiodeDAO
import no.nav.veilarbaktivitet.oppfolging.periode.finnOppfolgingsperiodeForArenaAktivitet
import no.nav.veilarbaktivitet.person.Person
import no.nav.veilarbaktivitet.person.UserInContext
import org.slf4j.LoggerFactory
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
    private val idMappingDAO: IdMappingDAO,
    private val aktivitetDAO: AktivitetDAO,
    private val migreringService: MigreringService,
    private val oppfolgingsperiodeDAO: OppfolgingsperiodeDAO,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    @PutMapping("/forhaandsorientering")
    open fun opprettFHO(
        @RequestBody forhaandsorientering: ForhaandsorienteringDTO?,
        @RequestParam arenaaktivitetId: ArenaId?
    ): ArenaAktivitetDTO {
        if (!authService.erInternBruker()) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Må være internbruker")
        }
        getInputFeilmelding(forhaandsorientering, arenaaktivitetId)
            .ifPresent { feilmelding: String? -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, feilmelding) }
        val fnr = userInContext?.getFnr()
            ?.orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "Finner ikke fnr") }
        if (fnr != null) {
            authService.sjekkTilgangTilPerson(fnr.eksternBrukerId())
        }
        val ident = authService.getInnloggetVeilederIdent()
        return arenaService.opprettFHO(arenaaktivitetId, fnr, forhaandsorientering, ident.get())
    }

    @GetMapping("/tiltak-raw")
    @AuthorizeFnr
    open fun hentAlleArenaAktiviteterRaw(): List<ArenaAktivitetDTO> {
        val fnr = userInContext.getFnr()
            .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "Må være på en bruker") }
        return arenaService.hentAktiviteter(fnr)
    }

    data class FnrDto (val fnr: String)

    @PostMapping("/tiltak")
    open fun postHentArenaAktiviteter(@RequestBody fnrDto: FnrDto) : List<ArenaAktivitetDTO> {
        val fnr = Person.fnr(fnrDto.fnr)
        authService.sjekkTilgangTilPerson(fnr.otherFnr())
        return innerHentArenaAktiviteter(fnr)
    }


    @GetMapping("/tiltak")
    @AuthorizeFnr
    open fun hentArenaAktiviteter(): List<ArenaAktivitetDTO> {
        val fnr = userInContext.getFnr()
            .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "Må være på en bruker") }
        return innerHentArenaAktiviteter(fnr)
    }

    fun innerHentArenaAktiviteter(fnr: Person.Fnr): List<ArenaAktivitetDTO> {

        val arenaAktiviteter = arenaService.hentAktiviteter(fnr)

        // Id-er og versjoner
        val ideer = arenaAktiviteter.map { arenaAktivitetDTO: ArenaAktivitetDTO -> ArenaId(arenaAktivitetDTO.id) }
        val idMappings = idMappingDAO.getMappings(ideer)
        val sisteIdMappinger = idMappingDAO.onlyLatestMappings(idMappings)
        val aktivitetsVersjoner = aktivitetDAO.getAktivitetsVersjoner(
            sisteIdMappinger.values.map(IdMappingWithAktivitetStatus::aktivitetId)
        )

        // Oppfolgingsperioder
        val oppfolgingsperioder = oppfolgingsperiodeDAO.getByAktorId(userInContext.aktorId)
        val arenaAktiviteterMedOppfolgingsperiode = arenaAktiviteter
            .map { it to oppfolgingsperioder.finnOppfolgingsperiodeForArenaAktivitet(it) }

        // Metrikker
        migreringService.countArenaAktiviteter(
            arenaAktiviteterMedOppfolgingsperiode,
            sisteIdMappinger
        )

        val skalFiltreresBort = migreringService.filtrerBortArenaTiltakHvisToggleAktiv(idMappings.keys)
        val filtrerteArenaAktiviteter = arenaAktiviteterMedOppfolgingsperiode
            // Bare vis arena aktiviteter som mangler id, dvs ikke er migrert
            .filter { skalFiltreresBort(it.first) }
            .filter { it.second != null } // Ikke vis arena-tiltak som ikke har oppfolgingsperiode
            .map { (arenaAktivitet: ArenaAktivitetDTO) ->
                val idMapping = sisteIdMappinger[ArenaId(arenaAktivitet.id)]
                if (idMapping != null) return@map arenaAktivitet
                    .withId(idMapping.aktivitetId.toString())
                    .withVersjon(aktivitetsVersjoner[idMapping.aktivitetId]!!)
                arenaAktivitet
            }
        logUmigrerteIder(filtrerteArenaAktiviteter)
        loggArenaTiltakUtenOppfolging(arenaAktiviteterMedOppfolgingsperiode)
        return filtrerteArenaAktiviteter
    }


    open fun logUmigrerteIder(arenaAktiviteter: List<ArenaAktivitetDTO>) {
        val umigrerteTiltaksIder = arenaAktiviteter
            .filter { aktivitet: ArenaAktivitetDTO -> aktivitet.type == ArenaAktivitetTypeDTO.TILTAKSAKTIVITET }
            .joinToString(",") { obj: ArenaAktivitetDTO -> obj.id }
        if (umigrerteTiltaksIder.isNotEmpty()) {
            log.info("Umigrerte tiltaksIdEr: $umigrerteTiltaksIder")
        }
    }

    open fun loggArenaTiltakUtenOppfolging(aktiviteter: List<Pair<ArenaAktivitetDTO, Oppfolgingsperiode?>>) {
        val tiltakIdErUtenOppfolgingsperiode = aktiviteter
            .filter { it.second == null }
            .joinToString(",") { it.first.id }
        log.info("Arenaaktiviteter uten oppfolgingsperiode: $tiltakIdErUtenOppfolgingsperiode")
    }

    @GetMapping("/harTiltak")
    @AuthorizeFnr
    open fun hentHarTiltak(): Boolean {
        val fnr = userInContext.getFnr()
            .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "Må være på en bruker") }
        return arenaService.harAktiveTiltak(fnr)
    }

    @PutMapping("/forhaandsorientering/lest")
    @AuthorizeFnr
    open fun lest(@RequestParam aktivitetId: ArenaId?): ArenaAktivitetDTO {
        val fnr = userInContext.getFnr()
            .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "Må være på en bruker") }
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
