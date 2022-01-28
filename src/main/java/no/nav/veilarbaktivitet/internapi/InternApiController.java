package no.nav.veilarbaktivitet.internapi;

import no.nav.veilarbaktivitet.internapi.api.InternalApi;
import no.nav.veilarbaktivitet.internapi.model.Aktivitet;
import no.nav.veilarbaktivitet.internapi.model.Oppfolgingsperiode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/internal/api/aktivitet")
public class InternApiController implements InternalApi {

    @Override
    public ResponseEntity<Aktivitet> hentAktivitet(Integer aktivitetId) {
      return null;
    }

    @Override
    public ResponseEntity<List<Oppfolgingsperiode>> hentAktiviteter(String aktorId) {
        return null;
    }
}
