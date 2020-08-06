package no.nav.veilarbaktivitet.api;

import no.nav.veilarbaktivitet.domain.AktivitetDTO;
import no.nav.veilarbaktivitet.domain.AktivitetsplanDTO;
import no.nav.veilarbaktivitet.domain.EtikettTypeDTO;
import no.nav.veilarbaktivitet.domain.KanalDTO;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.Arrays.asList;


@RestController
@RequestMapping("/aktivitet")
public interface AktivitetController {
    String ARENA_PREFIX = "ARENA";

    @GetMapping
    AktivitetsplanDTO hentAktivitetsplan();

    @GetMapping("/arena")
    List<ArenaAktivitetDTO> hentArenaAktiviteter();

    @PostMapping("/ny")
    AktivitetDTO opprettNyAktivitet(AktivitetDTO aktivitet, @RequestParam(defaultValue = "false") boolean automatisk);

    @PutMapping("/{id}")
    AktivitetDTO oppdaterAktivitet(AktivitetDTO aktivitet);

    @GetMapping("/{id}")
    AktivitetDTO hentAktivitet(@PathVariable("id") String aktivitetId);

    @GetMapping("/etiketter")
    default List<EtikettTypeDTO> hentEtiketter() {
        return asList(EtikettTypeDTO.values());
    }

    @GetMapping("/kanaler")
    default List<KanalDTO> hentKanaler() {
        return asList(KanalDTO.values());
    }

    @PutMapping("/{id}/etikett")
    AktivitetDTO oppdaterEtikett(AktivitetDTO aktivitet);


    @DeleteMapping("/{id}")
    void slettAktivitet(@PathVariable("id") String id);

    @PutMapping("/{id}/status")
    AktivitetDTO oppdaterStatus(AktivitetDTO aktivitet);

    @GetMapping("/{id}/versjoner")
    List<AktivitetDTO> hentAktivitetVersjoner(@PathVariable("id") String aktivitetId);

}
