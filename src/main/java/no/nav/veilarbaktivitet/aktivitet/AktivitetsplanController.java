package no.nav.veilarbaktivitet.aktivitet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.types.identer.EnhetId;
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr;
import no.nav.poao.dab.spring_a2_annotations.auth.OnlyInternBruker;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.EtikettTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDataMapperService;
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService;
import no.nav.veilarbaktivitet.person.UserInContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/aktivitet")
public class AktivitetsplanController {

    private final IAuthService authService;
    private final AktivitetAppService appService;
    private final AktivitetDataMapperService aktivitetDataMapperService;
    private final UserInContext userInContext;
    private final MigreringService migreringService;

    @GetMapping
    @AuthorizeFnr(auditlogMessage = "hent aktivitesplan")
    public AktivitetsplanDTO hentAktivitetsplan() {
        val userFnr = userInContext.getAktorId();
        boolean erEksternBruker = authService.erEksternBruker();
        val aktiviter = appService
                .hentAktiviteterForIdent(userFnr)
                .stream()
                .filter(this::filtrerKontorsperret)
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .toList();
        var filtrerteAktiviter = migreringService.visMigrerteArenaAktiviteterHvisToggleAktiv(aktiviter);

        return new AktivitetsplanDTO().setAktiviteter(filtrerteAktiviter);
    }

    @GetMapping("/{id}")
    @AuthorizeFnr(auditlogMessage = "hent en aktivitet")
    public AktivitetDTO hentAktivitet(@PathVariable("id") long aktivitetId) {
        boolean erEksternBruker = authService.erEksternBruker();

        return Optional.of(appService.hentAktivitet(aktivitetId))
                .filter(it -> it.getAktorId().equals(userInContext.getAktorId()))
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{id}/versjoner")
    @AuthorizeFnr(auditlogMessage = "hent aktivitet historikk")
    public List<AktivitetDTO> hentAktivitetVersjoner(@PathVariable("id") long aktivitetId) {
        return appService.hentAktivitetVersjoner(aktivitetId)
                .stream()
                .filter(it -> it.getAktorId().equals(userInContext.getAktorId()))
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, authService.erEksternBruker()))
                .toList();
    }

    @PostMapping("/ny")
    @AuthorizeFnr(auditlogMessage = "oppret aktivitet")
    public AktivitetDTO opprettNyAktivitet(@RequestBody AktivitetDTO aktivitet, @RequestParam(required = false, defaultValue = "false") boolean automatisk) {
        boolean erEksternBruker = authService.erEksternBruker();
        authService.sjekkTilgangTilPerson(userInContext.getAktorId().eksternBrukerId());

        return Optional.of(aktivitet)
                .map(aktivitetDataMapperService::mapTilAktivitetData)
                .map(aktivitetData -> aktivitetData.withAutomatiskOpprettet(automatisk))
                .map(appService::opprettNyAktivitet)
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(RuntimeException::new);
    }

    @PutMapping("/{id}")
    @AuthorizeFnr(auditlogMessage = "oppdater aktivitet")
    public AktivitetDTO oppdaterAktivitet(@RequestBody AktivitetDTO aktivitet) {
        boolean erEksternBruker = authService.erEksternBruker();

        return Optional.of(aktivitet)
                .map(aktivitetDataMapperService::mapTilAktivitetData)
                .map(appService::oppdaterAktivitet)
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(RuntimeException::new);
    }

    @PutMapping("/{id}/etikett")
    @AuthorizeFnr(auditlogMessage = "oppdater aktivitet etikett")
    public AktivitetDTO oppdaterEtikett(@RequestBody AktivitetDTO aktivitet) {
        boolean erEksternBruker = authService.erEksternBruker();

        return Optional.of(aktivitet)
                .map(aktivitetDataMapperService::mapTilAktivitetData)
                .map(appService::oppdaterEtikett)
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(RuntimeException::new);
    }


    @PutMapping("/{id}/status")
    @AuthorizeFnr(auditlogMessage = "oppdater aktivitet status")
    public AktivitetDTO oppdaterStatus(@RequestBody AktivitetDTO aktivitet) {
        boolean erEksternBruker = authService.erEksternBruker();

        return Optional.of(aktivitet)
                .map(aktivitetDataMapperService::mapTilAktivitetData)
                .map(appService::oppdaterStatus)
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(RuntimeException::new);
    }

    @PutMapping("/{aktivitetId}/referat")
    @AuthorizeFnr(auditlogMessage = "oppdater referat")
    @OnlyInternBruker
    public AktivitetDTO oppdaterReferat(@RequestBody AktivitetDTO aktivitetDTO) {

        return Optional.of(aktivitetDTO)
                .map(aktivitetDataMapperService::mapTilAktivitetData)
                .map(appService::oppdaterReferat)
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, false))
                .orElseThrow(RuntimeException::new);
    }

    @AuthorizeFnr(auditlogMessage = "publiser referat")
    @PutMapping("/{aktivitetId}/referat/publiser")
    public AktivitetDTO publiserReferat(@RequestBody AktivitetDTO aktivitetDTO) {
        return oppdaterReferat(aktivitetDTO);
    }

    @GetMapping("/etiketter")
    public List<EtikettTypeDTO> hentEtiketter() {
        return asList(EtikettTypeDTO.values());
    }

    @GetMapping("/kanaler")
    public List<KanalDTO> hentKanaler() {
        return asList(KanalDTO.values());
    }

    private boolean filtrerKontorsperret(AktivitetData aktivitet) {
        return aktivitet.getKontorsperreEnhetId() == null || authService.harTilgangTilEnhet(EnhetId.of(aktivitet.getKontorsperreEnhetId()));
    }
}
