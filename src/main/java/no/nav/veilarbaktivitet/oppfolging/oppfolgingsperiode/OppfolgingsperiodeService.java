package no.nav.veilarbaktivitet.oppfolging.oppfolgingsperiode;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OppfolgingsperiodeService {
    private final OppfolgingsperiodeDao dao;
    private final OppfolgingsperiodePersonService service;
    @Timed
    public long oppdater500brukere() {
        return dao
                .hentBrukereUtenOppfolgingsperiode(500)
                .stream().map(service::addOppfolgingsperioderForEnBruker).count();
    }

}
