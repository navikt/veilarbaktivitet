package no.nav.veilarbaktivitet.avtaltMedNav;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.domain.AktivitetData;
import no.nav.veilarbaktivitet.service.AktivitetAppService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/avtaltMedNav")
public class AvtaltMedNavController {

    private final AktivitetAppService service;


    @PutMapping
    public void markerSomAvtaltMedNav(@RequestBody AvtaltMedNavDTO avtaltMedNav, @RequestParam long aktivitetId) {
        AktivitetData aktivitet = service.hentAktivitet(aktivitetId);

        aktivitet.toBuilder().avtalt(true);

    }
}
