package no.nav.veilarbaktivitet.aktivitet

import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.EnhetId
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.poao.dab.spring_a2_annotations.auth.OnlyInternBruker
import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetsplanDTO
import no.nav.veilarbaktivitet.aktivitet.dto.EtikettTypeDTO
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDataMapperService
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService
import no.nav.veilarbaktivitet.person.UserInContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Slf4j
@RestController
@RequestMapping("/api/aktivitet")
class AktivitetsplanController(
    private val authService: IAuthService,
    private val appService: AktivitetAppService,
    private val aktivitetDataMapperService: AktivitetDataMapperService,
    private val userInContext: UserInContext,
    private val migreringService: MigreringService
) {
    @GetMapping
    @AuthorizeFnr(auditlogMessage = "hent aktivitesplan")
    fun hentAktivitetsplan(): AktivitetsplanDTO {
        val userFnr = userInContext.getAktorId()
        val erEksternBruker = authService.erEksternBruker()
        val aktiviter = appService
            .hentAktiviteterForIdent(userFnr)
            .stream()
            .filter { aktivitet: AktivitetData ->
                filtrerKontorsperret(
                    aktivitet
                )
            }
            .map { a: AktivitetData? ->
                AktivitetDTOMapper.mapTilAktivitetDTO(
                    a,
                    erEksternBruker
                )
            }
            .toList()
        val filtrerteAktiviter = migreringService.visMigrerteArenaAktiviteterHvisToggleAktiv(aktiviter)
        return AktivitetsplanDTO().setAktiviteter(filtrerteAktiviter)
    }

    @GetMapping("/{id}") //kan ikke bruke anotasjonen pga kasserings endepunktet
    fun hentAktivitet(@PathVariable("id") aktivitetId: Long): AktivitetDTO {
        val erEksternBruker = authService.erEksternBruker()
        return Optional.of(appService.hentAktivitet(aktivitetId))
            .filter { it: AktivitetData ->
                authService.sjekkTilgangTilPerson(it.aktorId.eksternBrukerId())
                true
            }
            .map { a: AktivitetData? ->
                AktivitetDTOMapper.mapTilAktivitetDTO(
                    a,
                    erEksternBruker
                )
            }
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.NOT_FOUND
                )
            }
    }

    @GetMapping("/{id}/versjoner")
    @AuthorizeFnr(auditlogMessage = "hent aktivitet historikk")
    fun hentAktivitetVersjoner(@PathVariable("id") aktivitetId: Long): List<AktivitetDTO> {
        return appService.hentAktivitetVersjoner(aktivitetId)
            .stream()
            .filter { it: AktivitetData -> it.aktorId == userInContext.getAktorId() }
            .map { a: AktivitetData? ->
                AktivitetDTOMapper.mapTilAktivitetDTO(
                    a,
                    authService.erEksternBruker()
                )
            }
            .toList()
    }

    @PostMapping("/ny")
    @AuthorizeFnr(auditlogMessage = "opprett aktivitet", allowlist = ["pto:veilarbdirigent"])
    fun opprettNyAktivitet(
        @RequestBody aktivitet: AktivitetDTO,
        @RequestParam(required = false, defaultValue = "false") automatisk: Boolean
    ): AktivitetDTO {
        val erEksternBruker = authService.erEksternBruker()
        return Optional.of(aktivitet)
            .map { aktivitetDTO: AktivitetDTO? ->
                aktivitetDataMapperService.mapTilAktivitetData(
                    aktivitetDTO
                )
            }
            .map { aktivitetData: AktivitetData ->
                aktivitetData.withAutomatiskOpprettet(
                    automatisk
                )
            }
            .map { aktivitetData: AktivitetData? ->
                appService.opprettNyAktivitet(
                    aktivitetData
                )
            }
            .map { a: AktivitetData? ->
                AktivitetDTOMapper.mapTilAktivitetDTO(
                    a,
                    erEksternBruker
                )
            }
            .orElseThrow { RuntimeException() }
    }

    @PutMapping("/{id}")
    @AuthorizeFnr(auditlogMessage = "oppdater aktivitet")
    fun oppdaterAktivitet(@RequestBody aktivitet: AktivitetDTO): AktivitetDTO {
        val erEksternBruker = authService.erEksternBruker()
        return Optional.of(aktivitet)
            .map { aktivitetDTO: AktivitetDTO? ->
                aktivitetDataMapperService.mapTilAktivitetData(
                    aktivitetDTO
                )
            }
            .map { aktivitet: AktivitetData? ->
                appService.oppdaterAktivitet(
                    aktivitet
                )
            }
            .map { a: AktivitetData? ->
                AktivitetDTOMapper.mapTilAktivitetDTO(
                    a,
                    erEksternBruker
                )
            }
            .orElseThrow { RuntimeException() }
    }

    @PutMapping("/{id}/etikett")
    @AuthorizeFnr(auditlogMessage = "oppdater aktivitet etikett")
    fun oppdaterEtikett(@RequestBody aktivitet: AktivitetDTO): AktivitetDTO {
        val erEksternBruker = authService.erEksternBruker()
        return Optional.of(aktivitet)
            .map { aktivitetDTO: AktivitetDTO? ->
                aktivitetDataMapperService.mapTilAktivitetData(
                    aktivitetDTO
                )
            }
            .map { aktivitet: AktivitetData? ->
                appService.oppdaterEtikett(
                    aktivitet
                )
            }
            .map { a: AktivitetData? ->
                AktivitetDTOMapper.mapTilAktivitetDTO(
                    a,
                    erEksternBruker
                )
            }
            .orElseThrow { RuntimeException() }
    }

    @PutMapping("/{id}/status")
    @AuthorizeFnr(auditlogMessage = "oppdater aktivitet status")
    fun oppdaterStatus(@RequestBody aktivitet: AktivitetDTO): AktivitetDTO {
        val erEksternBruker = authService.erEksternBruker()
        return Optional.of(aktivitet)
            .map { aktivitetDTO: AktivitetDTO? ->
                aktivitetDataMapperService.mapTilAktivitetData(
                    aktivitetDTO
                )
            }
            .map { aktivitet: AktivitetData? ->
                appService.oppdaterStatus(
                    aktivitet
                )
            }
            .map { a: AktivitetData? ->
                AktivitetDTOMapper.mapTilAktivitetDTO(
                    a,
                    erEksternBruker
                )
            }
            .orElseThrow { RuntimeException() }
    }

    @PutMapping("/{aktivitetId}/referat")
    @AuthorizeFnr(auditlogMessage = "oppdater referat")
    @OnlyInternBruker
    fun oppdaterReferat(@RequestBody aktivitetDTO: AktivitetDTO): AktivitetDTO {
        return Optional.of(aktivitetDTO)
            .map { aktivitetDTO: AktivitetDTO? ->
                aktivitetDataMapperService.mapTilAktivitetData(
                    aktivitetDTO
                )
            }
            .map { aktivitet: AktivitetData? ->
                appService.oppdaterReferat(
                    aktivitet
                )
            }
            .map { a: AktivitetData? ->
                AktivitetDTOMapper.mapTilAktivitetDTO(
                    a,
                    false
                )
            }
            .orElseThrow { RuntimeException() }
    }

    @AuthorizeFnr(auditlogMessage = "publiser referat")
    @OnlyInternBruker
    @PutMapping("/{aktivitetId}/referat/publiser")
    fun publiserReferat(@RequestBody aktivitetDTO: AktivitetDTO): AktivitetDTO {
        return oppdaterReferat(aktivitetDTO)
    }

    @GetMapping("/etiketter")
    fun hentEtiketter(): List<EtikettTypeDTO> {
        return Arrays.asList(*EtikettTypeDTO.values())
    }

    @GetMapping("/kanaler")
    fun hentKanaler(): List<KanalDTO> {
        return Arrays.asList(*KanalDTO.values())
    }

    private fun filtrerKontorsperret(aktivitet: AktivitetData): Boolean {
        return aktivitet.kontorsperreEnhetId == null || authService.harTilgangTilEnhet(EnhetId.of(aktivitet.kontorsperreEnhetId))
    }
}
