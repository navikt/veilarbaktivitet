package no.nav.veilarbaktivitet.internapi;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.internapi.api.InternalApi;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet;
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT));

        return ResponseEntity.of(Optional.of(aktivitet));
    }

    @Override
    public ResponseEntity<List<Aktivitet>> hentAktiviteter(String aktorId, UUID oppfolgingsperiodeId) {
        List<Aktivitet> aktiviteter = Optional.of(internApiService.hentAktiviteter(aktorId, oppfolgingsperiodeId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT))
                .stream()
                .map(InternAktivitetMapper::mapTilAktivitet)
                .toList();
        return ResponseEntity.of(Optional.of(aktiviteter));
    }
}
