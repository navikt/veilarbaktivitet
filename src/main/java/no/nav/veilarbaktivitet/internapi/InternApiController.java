package no.nav.veilarbaktivitet.internapi;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.internapi.api.InternalApi;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet;
import no.nav.veilarbaktivitet.internapi.model.Oppfolgingsperiode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class InternApiController implements InternalApi {

    private final InternApiService internApiService;

    @Override
    public ResponseEntity<Aktivitet> hentAktivitet(Integer aktivitetId) {
        Aktivitet aktivitet = Optional.of(internApiService.hentAktivitet(aktivitetId))
                .map(InternAktivitetMapper::mapTilAktivitet)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return ResponseEntity.of(Optional.of(aktivitet));
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
