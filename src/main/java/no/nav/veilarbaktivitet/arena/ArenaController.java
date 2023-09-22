package no.nav.veilarbaktivitet.arena;

import lombok.RequiredArgsConstructor;
import no.nav.poao.dab.spring_a2_annotations.auth.AuthorizeFnr;
import no.nav.poao.dab.spring_auth.IAuthService;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitetskort.MigreringService;
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMapping;
import no.nav.veilarbaktivitet.aktivitetskort.idmapping.IdMappingDAO;
import no.nav.veilarbaktivitet.arena.model.ArenaAktivitetDTO;
import no.nav.veilarbaktivitet.arena.model.ArenaId;
import no.nav.veilarbaktivitet.avtalt_med_nav.ForhaandsorienteringDTO;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.person.UserInContext;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Transactional
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/arena")
public class ArenaController {
    private final UserInContext userInContext;
    private final IAuthService authService;
    private final ArenaService arenaService;
    private final IdMappingDAO idMappingDAO;
    private final AktivitetDAO aktivitetDAO;

    private final MigreringService migreringService;


    @PutMapping("/forhaandsorientering")
    public ArenaAktivitetDTO opprettFHO(@RequestBody ForhaandsorienteringDTO forhaandsorientering, @RequestParam ArenaId arenaaktivitetId) {
        if (!authService.erInternBruker()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Må være internbruker");
        }

        getInputFeilmelding(forhaandsorientering, arenaaktivitetId)
                .ifPresent( feilmelding -> {throw new ResponseStatusException(HttpStatus.BAD_REQUEST, feilmelding);});

        Person.Fnr fnr = userInContext.getFnr()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finner ikke fnr"));
        authService.sjekkTilgangTilPerson(fnr.eksternBrukerId());

        var ident = authService.getInnloggetVeilederIdent();

        return arenaService.opprettFHO(arenaaktivitetId, fnr, forhaandsorientering, ident.get());
    }



    @GetMapping("/tiltak")
    @AuthorizeFnr
    public List<ArenaAktivitetDTO> hentArenaAktiviteter() {
        Person.Fnr fnr = userInContext.getFnr().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Må være på en bruker"));
        var arenaAktiviteter = arenaService.hentAktiviteter(fnr);
        var ideer = arenaAktiviteter.stream().map(arenaAktivitetDTO -> new ArenaId(arenaAktivitetDTO.getId())).toList();
        var idMappings = idMappingDAO.getMappings(ideer);
        var aktivitetsVersjoner = aktivitetDAO.getAktivitetsVersjoner(idMappings.values().stream().map(IdMapping::getAktivitetId).toList());
        return arenaAktiviteter
            .stream()
                // Bare vis arena aktiviteter som mangler id, dvs ikke er migrert
                .filter(migreringService.filtrerBortArenaTiltakHvisToggleAktiv(idMappings.keySet()))
                .map(arenaAktivitet -> {
                    var idMapping = idMappings.get(new ArenaId(arenaAktivitet.getId()));
                    if (idMapping != null)
                        return arenaAktivitet
                            .withId(String.valueOf(idMapping.getAktivitetId()))
                            .withVersjon(aktivitetsVersjoner.get(idMapping.getAktivitetId()));
                    return arenaAktivitet;
                })
                .toList();
    }


    @GetMapping("/harTiltak")
    @AuthorizeFnr
    boolean hentHarTiltak() {
        Person.Fnr fnr = userInContext.getFnr().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Må være på en bruker"));
        return arenaService.harAktiveTiltak(fnr);
    }

    @PutMapping("/forhaandsorientering/lest")
    @AuthorizeFnr
    ArenaAktivitetDTO lest(@RequestParam ArenaId aktivitetId) {
        Person.Fnr fnr = userInContext.getFnr().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Må være på en bruker"));
        return arenaService.markerSomLest(fnr, aktivitetId);
    }

    private Optional<String> getInputFeilmelding(ForhaandsorienteringDTO forhaandsorientering, ArenaId arenaaktivitetId) {
        if(arenaaktivitetId == null || arenaaktivitetId.id() == null || arenaaktivitetId.id().isBlank()) {
            return Optional.of("arenaaktivitetId kan ikke være null eller tom");
        }

        if (forhaandsorientering == null) {
            return Optional.of("forhaandsorientering kan ikke være null");
        }

        if (forhaandsorientering.getType() == null) {
            return Optional.of("forhaandsorientering.type kan ikke være null");
        }

        if (forhaandsorientering.getTekst() == null || forhaandsorientering.getTekst().isEmpty()) {
            return Optional.of("forhaandsorientering.tekst kan ikke være null eller tom");
        }
        return Optional.empty();
    }
}
