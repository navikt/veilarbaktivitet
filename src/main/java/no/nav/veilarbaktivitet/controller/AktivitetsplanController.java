package no.nav.veilarbaktivitet.controller;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.mappers.AktivitetDataMapper;
import no.nav.veilarbaktivitet.service.AktivitetAppService;
import no.nav.veilarbaktivitet.service.BrukerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static no.nav.veilarbaktivitet.domain.AktivitetStatus.AVBRUTT;
import static no.nav.veilarbaktivitet.domain.AktivitetStatus.FULLFORT;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/aktivitet")
public class AktivitetsplanController {

    private final AktivitetAppService appService;
    private final HttpServletRequest requestProvider;

    @GetMapping
    public AktivitetsplanDTO hentAktivitetsplan() {
        val aktiviter = appService
                .hentAktiviteterForIdent(getContextUserIdent())
                .stream()
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .collect(Collectors.toList());

        return new AktivitetsplanDTO().setAktiviteter(aktiviter);
    }

    @GetMapping("/{id}")
    public AktivitetDTO hentAktivitet(@PathVariable("id") String aktivitetId) {
        return Optional.of(appService.hentAktivitet(Long.parseLong(aktivitetId)))
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // TODO: 25/02/2021 slett denne etter flytting
    @Deprecated
    @GetMapping("/arena")
    public List<ArenaAktivitetDTO> hentArenaAktiviteter() {
        return getFnr()
                .map(appService::hentArenaAktiviteter)
                .orElseThrow(RuntimeException::new);
    }

    // TODO: 25/02/2021 slett denne etter flytting
    @Deprecated
    @GetMapping("/harTiltak")
    public boolean hentHarTiltak() {
        return getFnr()
                .map(appService::hentArenaAktiviteter)
                .orElseThrow(RuntimeException::new)
                .stream()
                .map(ArenaAktivitetDTO::getStatus)
                .anyMatch(status -> status != AVBRUTT && status != FULLFORT);
    }

    @GetMapping("/{id}/versjoner")
    public List<AktivitetDTO> hentAktivitetVersjoner(@PathVariable("id") String aktivitetId) {
        return Optional.of(aktivitetId)
                .map(Long::parseLong)
                .map(appService::hentAktivitetVersjoner)
                .map(aktivitetList -> aktivitetList
                        .stream()
                        .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                        .collect(Collectors.toList())
                ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/ny")
    public AktivitetDTO opprettNyAktivitet(@RequestBody AktivitetDTO aktivitet, @RequestParam(required = false, defaultValue = "false") boolean automatisk) {
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(aktivitetData -> aktivitetData.withAutomatiskOpprettet(automatisk))
                .map((aktivitetData) -> appService.opprettNyAktivitet(getContextUserIdent(), aktivitetData))
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @PutMapping("/{id}")
    public AktivitetDTO oppdaterAktivitet(@RequestBody AktivitetDTO aktivitet) {
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterAktivitet)
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @PutMapping("/{id}/etikett")
    public AktivitetDTO oppdaterEtikett(@RequestBody AktivitetDTO aktivitet) {
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterEtikett)
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }


    @PutMapping("/{id}/status")
    public AktivitetDTO oppdaterStatus(@RequestBody AktivitetDTO aktivitet) {
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterStatus)
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @PutMapping("/{aktivitetId}/referat")
    public AktivitetDTO oppdaterReferat(@RequestBody AktivitetDTO aktivitetDTO) {
        return Optional.of(aktivitetDTO)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(appService::oppdaterReferat)
                .map(AktivitetDTOMapper::mapTilAktivitetDTO)
                .orElseThrow(RuntimeException::new);
    }

    @PutMapping("/{aktivitetId}/referat/publiser")
    public AktivitetDTO publiserReferat(@RequestBody AktivitetDTO aktivitetDTO) {
        return oppdaterReferat(aktivitetDTO);
    }

    private Person getContextUserIdent() {
        if (BrukerService.erEksternBruker()) {
            return SubjectHandler.getIdent().map(Person::fnr).orElseThrow(RuntimeException::new);
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

    private Optional<Person.Fnr> getFnr() {
        return Optional.of(getContextUserIdent())
                .filter((person) -> person instanceof Person.Fnr)
                .map((person) -> (Person.Fnr) person);
    }
}
