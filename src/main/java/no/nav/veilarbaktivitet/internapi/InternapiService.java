package no.nav.veilarbaktivitet.internapi;

import no.nav.veilarbaktivitet.internapi.model.Aktivitet;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class InternapiService {
    public Optional<Aktivitet> hentAktivitet(Integer aktivitetId) {
        return Optional.empty();
    }
}
