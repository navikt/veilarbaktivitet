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
                .stream()
                .map(service::addOppfolgingsperioderForEnBruker)
                .toList() //kan ikke bruke count() her da den hopper over mappen.
                .size();
    }

    @Timed
    public void samskjorAktiviter(int maksantall) {
        int aktiitet_id = dao.hentSisteOppdaterteAktivitet();
        if(aktiitet_id > 13_317_753) {
            log.info("ferdig med samskjoreing");
            return;
        }
        dao.matchPeriodeForAktivitet(aktiitet_id, aktiitet_id + maksantall);
        dao.oppdaterSiteOppdaterteAktivitet(aktiitet_id + maksantall);
    }
}
