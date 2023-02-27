package no.nav.veilarbaktivitet.aktivitet;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.EtikettTypeDTO;
import no.nav.veilarbaktivitet.aktivitet.dto.KanalDTO;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.aktivitet.mappers.AktivitetDataMapper;
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService;
import no.nav.veilarbaktivitet.person.Person;
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
    private final HttpServletRequest requestProvider;

    private final MigreringService migreringService;

    @GetMapping
    public AktivitetsplanDTO hentAktivitetsplan() {
        boolean erEksternBruker = authService.erEksternBruker();
        val aktiviter = appService
                .hentAktiviteterForIdent(getContextUserIdent())
                .stream()
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .filter(migreringService.visMigrerteArenaAktiviteterHvisToggleAktiv())
                .toList();

        return new AktivitetsplanDTO().setAktiviteter(aktiviter);
    }

    @GetMapping("/{id}")
    public AktivitetDTO hentAktivitet(@PathVariable("id") String aktivitetId) {
        boolean erEksternBruker = authService.erEksternBruker();
        return Optional.of(appService.hentAktivitet(Long.parseLong(aktivitetId)))
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{id}/versjoner")
    public List<AktivitetDTO> hentAktivitetVersjoner(@PathVariable("id") String aktivitetId) {
        boolean erEksternBruker = authService.erEksternBruker();
        return Optional.of(aktivitetId)
                .map(Long::parseLong)
                .map(appService::hentAktivitetVersjoner)
                .map(aktivitetList -> aktivitetList
                        .stream()
                        .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                        .toList()
                ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/ny")
    public AktivitetDTO opprettNyAktivitet(@RequestBody AktivitetDTO aktivitet, @RequestParam(required = false, defaultValue = "false") boolean automatisk) {
        boolean erEksternBruker = authService.erEksternBruker();
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(aktivitetData -> aktivitetData.withAutomatiskOpprettet(automatisk))
                .map(aktivitetData -> appService.opprettNyAktivitet(getContextUserIdent(), aktivitetData))
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(RuntimeException::new);
    }

    @PutMapping("/{id}")
    public AktivitetDTO oppdaterAktivitet(@RequestBody AktivitetDTO aktivitet) {
        boolean erEksternBruker = authService.erEksternBruker();
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterAktivitet)
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(RuntimeException::new);
    }

    @PutMapping("/{id}/etikett")
    public AktivitetDTO oppdaterEtikett(@RequestBody AktivitetDTO aktivitet) {
        boolean erEksternBruker = authService.erEksternBruker();
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterEtikett)
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(RuntimeException::new);
    }


    @PutMapping("/{id}/status")
    public AktivitetDTO oppdaterStatus(@RequestBody AktivitetDTO aktivitet) {
        boolean erEksternBruker = authService.erEksternBruker();
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterStatus)
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(RuntimeException::new);
    }

    @PutMapping("/{aktivitetId}/referat")
    public AktivitetDTO oppdaterReferat(@RequestBody AktivitetDTO aktivitetDTO) {
        boolean erEksternBruker = authService.erEksternBruker();
        return Optional.of(aktivitetDTO)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterReferat)
                .map(a -> AktivitetDTOMapper.mapTilAktivitetDTO(a, erEksternBruker))
                .orElseThrow(RuntimeException::new);
    }

    @PutMapping("/{aktivitetId}/referat/publiser")
    public AktivitetDTO publiserReferat(@RequestBody AktivitetDTO aktivitetDTO) {
        return oppdaterReferat(aktivitetDTO);
    }

    // TODO: 30/01/2023  Bruk UserInContext istedet

    private Person getContextUserIdent() {
        if (authService.erEksternBruker()) {
            return Person.fnr(authService.getLoggedInnUser().get());
        }

        Optional<Person> fnr = Optional.ofNullable(requestProvider.getParameter("fnr")).map(Person::fnr);
        Optional<Person> aktorId = Optional.ofNullable(requestProvider.getParameter("aktorId")).map(Person::aktorId);
        return fnr.orElseGet(() -> aktorId.orElseThrow(RuntimeException::new));
    }

    @GetMapping("/etiketter")
    public List<EtikettTypeDTO> hentEtiketter() {
        return asList(EtikettTypeDTO.values());
    }

    @GetMapping("/kanaler")
    public List<KanalDTO> hentKanaler() {
        return asList(KanalDTO.values());
    }
}
