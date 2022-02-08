package no.nav.veilarbaktivitet.internapi;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.aktivitet.AktivitetDAO;
import no.nav.veilarbaktivitet.aktivitet.domain.AktivitetData;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InternApiService {
    private final AktivitetDAO aktivitetDAO;

    public AktivitetData hentAktivitet(Integer aktivitetId) {
        return aktivitetDAO.hentAktivitet(aktivitetId);
    }
}
