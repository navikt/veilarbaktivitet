package no.nav.veilarbaktivitet.oppfolging.oppfolgingsperiode;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.oppfolging.client.OppfolgingV2Client;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.time.temporal.ChronoUnit.MILLIS;

@Service
@RequiredArgsConstructor
@Slf4j
public class OppfolgingsperiodePersonService {
    private final OppfolgingsperiodeDao dao;
    private final OppfolgingV2Client client;

    @Timed(value = "oppfolgingsperiodeAdder", histogram = true)
    public boolean addOppfolgingsperioderForEnBruker(Person.AktorId aktorId) {
        List<OppfolgingPeriodeMinimalDTO> oppfolgingperioder;
        try {
            oppfolgingperioder = client
                    .hentOppfolgingsperioder(aktorId)
                    .orElse(List.of())
                    .stream()
                    .map(it -> {
                        it.setSluttDato(it.getSluttDato().truncatedTo(MILLIS));
                        it.setStartDato(it.getSluttDato().truncatedTo(MILLIS));
                        return  it;
                    })
                    .toList(); //Finnes bruker uten oppfolginsperioder

            if (oppfolgingperioder.isEmpty()) {
                int raderOppdatert = dao.setTilInngenPeriodePaaBruker(aktorId);
                log.warn("ingen oppfolingsperioder for aktørid: {} antall aktivteter oppdatert: {}", aktorId.get(), raderOppdatert);
                return false;
            }

        } catch (IngenGjeldendeIdentException e) {
            dao.setUkjentAktorId(aktorId);
            log.warn("ukjent aktorId {}", aktorId.get());
            return false;
        }

        for (OppfolgingPeriodeMinimalDTO oppfolgingsperiode : oppfolgingperioder) {
            oppfolgingsperiode.setSluttDato(oppfolgingsperiode.getSluttDato().truncatedTo(MILLIS));
            long raderOppdatert = dao.oppdaterAktiviteterForPeriode(aktorId, oppfolgingsperiode.getStartDato(), oppfolgingsperiode.getSluttDato(), oppfolgingsperiode.getUuid());
            if (raderOppdatert > 0) {
                log.info("lagt til oppfolgingsperiode={} i {} antall aktivitetsversjoner for aktorid={}", oppfolgingsperiode.getUuid(), raderOppdatert, aktorId.get());
            }
        }

        oppfolgingperioder
                .stream()
                .filter(it -> it.getSluttDato() != null)
                .forEach(oppfolgingsperiode -> {
                            long raderOppdatert = dao.oppdaterAktiviteterMedSluttdato(aktorId, oppfolgingsperiode.getSluttDato(), oppfolgingsperiode.getUuid());
                            if (raderOppdatert > 0) {
                                log.info("lagt til oppfolgingsperiode={} i {} antall aktivitetsversjoner for aktorid={} basert på sluttdato", oppfolgingsperiode.getUuid(), raderOppdatert, aktorId.get());
                            }
                        }
                );

        int antallUtenOppfolingsperiode = dao.setOppfolgingsperiodeTilUkjentForGamleAktiviteterUtenOppfolgingsperiode(aktorId);
        if (antallUtenOppfolingsperiode != 0) {
            log.warn("Oppdaterete aktivitere med ukjent oppfolgingsperiode for aktorid {} antall: {} brukeren har {} perioder", aktorId.get(), antallUtenOppfolingsperiode, oppfolgingperioder.size());
        }

        return true;
    }
}
