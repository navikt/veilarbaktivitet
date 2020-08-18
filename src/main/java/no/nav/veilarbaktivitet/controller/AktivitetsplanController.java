package no.nav.veilarbaktivitet.controller;

import lombok.val;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.veilarbaktivitet.domain.*;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.mappers.AktivitetDTOMapper;
import no.nav.veilarbaktivitet.mappers.AktivitetDataMapper;
import no.nav.veilarbaktivitet.service.AktivitetAppService;
import no.nav.veilarbaktivitet.service.BrukerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;


@RestController
@RequestMapping("/api/aktivitet")
public class AktivitetsplanController {

    private final AktivitetAppService appService;
    private final HttpServletRequest requestProvider;

    @Autowired
    public AktivitetsplanController(
            AktivitetAppService appService,
            HttpServletRequest requestProvider
    ) {
        this.appService = appService;
        this.requestProvider = requestProvider;
    }

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

    @GetMapping("/arena")
    public List<ArenaAktivitetDTO> hentArenaAktiviteter() {
        return getFnr()
                .map(appService::hentArenaAktiviteter)
                .orElseThrow(RuntimeException::new);
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
    public AktivitetDTO opprettNyAktivitet(@RequestBody AktivitetDTO aktivitet) {
        return Optional.of(aktivitet)
                .map(AktivitetDataMapper::mapTilAktivitetData)
                .map(aktivitetData -> aktivitetData.withAutomatiskOpprettet(false))
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

    @DeleteMapping("/{id}")
    public void slettAktivitet(@PathVariable("id") String id) {
        appService.slettAktivitet(Long.parseLong(id));
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

    @PutMapping
    @Path("/{aktivitetId}/referat/publiser")
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
