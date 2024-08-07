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

import static no.nav.veilarbaktivitet.internapi.InternAktivitetMapperKt.mapTilAktivitet;

@Controller
@RequiredArgsConstructor
public class InternApiController implements InternalApi {

    private final InternApiService internApiService;

    @Override
    public ResponseEntity<List<Aktivitet>> hentAktiviteter(String aktorId) {
        List<Aktivitet> aktiviteter = Optional.of(internApiService.hentAktiviteter(aktorId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NO_CONTENT))
                .stream()
                .map(it -> mapTilAktivitet(it))
                .toList();
        return ResponseEntity.of(Optional.of(aktiviteter));
    }
}
