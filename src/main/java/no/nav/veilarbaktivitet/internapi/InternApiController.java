package no.nav.veilarbaktivitet.internapi;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.internapi.api.InternalApi;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet;
import no.nav.veilarbaktivitet.internapi.model.Mote;
import no.nav.veilarbaktivitet.internapi.model.Oppfolgingsperiode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class InternApiController implements InternalApi {

    private final InternapiService internapiService;

    @Override
    public ResponseEntity<Aktivitet> hentAktivitet(Integer aktivitetId) {
        return ResponseEntity.of(internapiService.hentAktivitet(aktivitetId));

    }

    @Override
    public ResponseEntity<List<Aktivitet>> hentAktiviteter(String aktorId) {
        return null;
    }

    @Override
    public ResponseEntity<Oppfolgingsperiode> hentOppfolgingsperiode(UUID oppfolgingsperiodeId) {
        return null;
    }

    @Override
    public ResponseEntity<List<Oppfolgingsperiode>> hentOppfolgingsperioder(String aktorId) {
        return null;
    }
}
