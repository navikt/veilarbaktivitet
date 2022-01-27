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

@Service
@RequiredArgsConstructor
@Slf4j
public class OppfolgingsperiodePersonService {
    private final OppfolgingsperiodeDao dao;
    private final OppfolgingV2Client client;

    @Timed(value = "oppfolgingsperiodeAdder", histogram = true)
    public boolean addOppfolgingsperioderForEnBruker(Person.AktorId aktorId) {
        log.info("oppdaterer oppfolgingsperioder for aktorid: {} ", aktorId);
        List<OppfolgingPeriodeMinimalDTO> oppfolgingperioder;
        try {
            oppfolgingperioder = client
                    .hentOppfolgingsperioder(aktorId)
                    .orElse(List.of()); //Finnes bruker uten oppfolginsperioder

        } catch (IngenGjeldendeIdentException e) {
            dao.setUkjentAktorId(aktorId);
            log.warn("ukjent aktorId {}", aktorId);
            return false;
        }
        for (OppfolgingPeriodeMinimalDTO oppfolgingsperiode : oppfolgingperioder) {
            long raderOppdatert = dao.oppdaterAktiviteterForPeriode(aktorId, oppfolgingsperiode.getStartDato(), oppfolgingsperiode.getSluttDato(), oppfolgingsperiode.getUuid());
            log.info("lagt til oppfolgingsperiode={} i {} antall aktivitetsversjoner for aktorid={}", oppfolgingsperiode.getUuid(), raderOppdatert, aktorId.get());
        }

        oppfolgingperioder
                .stream()
                .filter(it -> it.getSluttDato() != null)
                .forEach(oppfolgingsperiode -> {
                            long raderOppdatert = dao.oppdaterAktiviteterMedSluttdato(aktorId, oppfolgingsperiode.getSluttDato(), oppfolgingsperiode.getUuid());
                            log.info("lagt til oppfolgingsperiode={} i {} antall aktivitetsversjoner for aktorid={} basert på sluttdato", oppfolgingsperiode.getUuid(), raderOppdatert, aktorId.get());
                        }
                );

        dao.setOppfolgingsperiodeTilUkjentForGamleAktiviteterUtenOppfolgingsperiode(aktorId);

        return true;
    }
}
