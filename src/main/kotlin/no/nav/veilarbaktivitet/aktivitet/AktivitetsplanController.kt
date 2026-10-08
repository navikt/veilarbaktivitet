package no.nav.veilarbaktivitet.aktivitet

import java.util.Arrays
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.EnhetId
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr
import no.nav.poao.dab.spring_a2_annotations.auth.OnlyInternBruker
import no.nav.poao.dab.spring_auth.IAuthService
import no.nav.poao.dab.spring_auth.TilgangsType
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetsplanDTO
import no.nav.veilarbaktivitet.aktivitet.dto.EtikettTypeDTO
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDataMapperService
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService
import no.nav.veilarbaktivitet.config.AktivitetResource
import no.nav.veilarbaktivitet.config.OppfolgingsperiodeResource
import no.nav.veilarbaktivitet.kvp.KvpService
import no.nav.veilarbaktivitet.person.UserInContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@Slf4j
@RestController
@RequestMapping("/api/aktivitet")
class AktivitetsplanController(
    private val authService: IAuthService,
    private val appService: AktivitetAppService,
    private val aktivitetDataMapperService: AktivitetDataMapperService,
    private val userInContext: UserInContext,
    private val migreringService: MigreringService,
    private val kvpService: KvpService,
) {
    @Deprecated("Bruk graphql endepunkt")
    @GetMapping
    @AuthorizeFnr(auditlogMessage = "hent aktivitesplan")
    fun hentAktivitetsplan(): AktivitetsplanDTO {
        val userFnr = userInContext.getAktorId()
        val erEksternBruker = authService.erEksternBruker()
        val aktiviter = appService
            .hentAktiviteterForIdent(userFnr)
            .stream()
            .filter { aktivitet -> filtrerKontorsperret(aktivitet) }
            .map { a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker) }
            .toList()
        val filtrerteAktiviter = migreringService.visMigrerteArenaAktiviteterHvisToggleAktiv(aktiviter)
        return AktivitetsplanDTO().setAktiviteter(filtrerteAktiviter)
    }

    @GetMapping("/{id}") //kan ikke bruke anotasjonen pga kasserings endepunktet
    @AuthorizeFnr(auditlogMessage = "hent aktivitet", resourceIdParamName = "id", resourceType = AktivitetResource::class, tilgangsType = TilgangsType.LESE)
    fun hentAktivitet(@PathVariable("id") aktivitetId: Long): AktivitetDTO {
        val erEksternBruker = authService.erEksternBruker()
        return Optional.of(appService.hentAktivitet(aktivitetId))
            .filter {
                authService.sjekkTilgangTilPerson(it.aktorId.eksternBrukerId(), TilgangsType.LESE)
                true
            }
            .map { a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker) }
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND)
            }
    }

    @GetMapping("/{id}/versjoner")
    @AuthorizeFnr(auditlogMessage = "hent aktivitet historikk", resourceIdParamName = "id", resourceType = AktivitetResource::class)
    fun hentAktivitetVersjoner(@PathVariable("id") aktivitetId: Long): List<AktivitetDTO> {
        return appService.hentAktivitetVersjoner(aktivitetId)
            .stream()
            .map { a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, authService.erEksternBruker()) }
            .toList()
    }

    @PostMapping("/{oppfolgingsperiodeId}/ny")
    @AuthorizeFnr(auditlogMessage = "opprett aktivitet", allowlist = ["dab:veilarbdirigent"], resourceIdParamName = "oppfolgingsperiodeId", resourceType = OppfolgingsperiodeResource::class)
    fun opprettNyAktivitetPaOppfolgingsPeriode(
        @RequestBody aktivitet: AktivitetDTO,
        @RequestParam(required = false, defaultValue = "false") automatisk: Boolean
    ): AktivitetDTO {
        if (authService.erInternBruker()) {
            sjekkKvpTilgangTilPerson()
        }
        return aktivitetDataMapperService.mapTilOpprettAktivitetData(aktivitet, automatisk)
            .let { aktivitetData -> appService.opprettNyAktivitet(aktivitetData) }
            .let { a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, authService.erEksternBruker()) }
    }

    @PutMapping("/{id}")
    @AuthorizeFnr(auditlogMessage = "oppdater aktivitet", resourceIdParamName = "id", resourceType = AktivitetResource::class)
    fun oppdaterAktivitet(@RequestBody aktivitet: AktivitetDTO): AktivitetDTO {
        val erEksternBruker = authService.erEksternBruker()
        return Optional.of(aktivitet)
            .map { aktivitetDTO -> aktivitetDataMapperService.mapTilOppdaterAktivitetData(aktivitetDTO) }
            .map { aktivitet -> appService.oppdaterAktivitet(aktivitet) }
            .map { a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker) }
            .orElseThrow { RuntimeException() }
    }

    @PutMapping("/{id}/etikett")
    @AuthorizeFnr(auditlogMessage = "oppdater aktivitet etikett", resourceIdParamName = "id", resourceType = AktivitetResource::class)
    fun oppdaterEtikett(@RequestBody aktivitet: AktivitetDTO): AktivitetDTO {
        val erEksternBruker = authService.erEksternBruker()
        return Optional.of(aktivitet)
            .map { aktivitetDTO -> aktivitetDataMapperService.mapTilOppdaterEtikett(aktivitetDTO) }
            .map { aktivitet -> appService.oppdaterEtikett(aktivitet) }
            .map { a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker) }
            .orElseThrow { RuntimeException() }
    }

    @PutMapping("/{id}/status")
    @AuthorizeFnr(auditlogMessage = "oppdater aktivitet status", resourceIdParamName = "id", resourceType = AktivitetResource::class)
    fun oppdaterStatus(@RequestBody aktivitet: AktivitetDTO): AktivitetDTO {
        val erEksternBruker = authService.erEksternBruker()
        return Optional.of(aktivitet)
            .map { aktivitetDTO -> aktivitetDataMapperService.mapTilOppdaterStatus(aktivitetDTO) }
            .map { aktivitet -> appService.oppdaterStatus(aktivitet) }
            .map { a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker) }
            .orElseThrow { RuntimeException() }
    }

    @PutMapping("/{aktivitetId}/referat")
    @AuthorizeFnr(auditlogMessage = "oppdater referat", resourceIdParamName = "aktivitetId", resourceType = AktivitetResource::class)
    @OnlyInternBruker
    fun oppdaterReferat(@RequestBody aktivitetDTO: AktivitetDTO): AktivitetDTO {
        return Optional.of(aktivitetDTO)
            .map { aktivitetDTO -> aktivitetDataMapperService.mapTilOppdaterReferat(aktivitetDTO) }
            .map { aktivitet -> appService.oppdaterReferat(aktivitet) }
            .map { a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, false) }
            .orElseThrow { RuntimeException() }
    }

    @AuthorizeFnr(auditlogMessage = "publiser referat", resourceIdParamName = "aktivitetId", resourceType = AktivitetResource::class)
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

    private fun sjekkKvpTilgangTilPerson() {
        val aktorId = userInContext.getAktorId()
        val kontorsperreEnhet = kvpService.getKontorSperreEnhet(aktorId)?.getOrNull()

        kontorsperreEnhet?.let {
            if (!authService.harTilgangTilEnhet(it)) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Veileder har ikke tilgang til bruker med KVP")
            }
        }
    }
}
