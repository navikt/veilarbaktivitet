package no.nav.veilarbaktivitet.oppfolging.oppfolgingsperiode;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.IngenGjeldendeIdentException;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingV2Client;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OppfolgingsperiodeService {
    private final OppfolgingsperiodeDao dao;
    private final OppfolgingV2Client client;

    @Transactional
    @Timed(value = "oppfolgingsperiodeAdder", histogram = true)
    public boolean addOppfolgingsperioderForEnBruker() {
        Person.AktorId aktorId = dao.hentEnBrukerUtenOppfolgingsperiode();

        if (aktorId == null) {
            log.info("Fant ingen brukere uten oppfølgingsperiode");
            return false;
        }

        List<OppfolgingPeriodeMinimalDTO> oppfolgingperioder;
        try {
            oppfolgingperioder = client
                    .hentOppfolgingsperioder(aktorId)
                    .orElse(List.of()); //Finnes bruker uten oppfolginsperioder

        } catch (IngenGjeldendeIdentException e) {
            dao.setUkjentAktorId(aktorId);
            log.warn("ukjent aktorId {}", aktorId);
            return true;
        }
        for (OppfolgingPeriodeMinimalDTO oppfolgingsperiode : oppfolgingperioder) {
            long raderOppdatert = dao.oppdaterAktiviteterForPeriode(aktorId, oppfolgingsperiode.getStartDato(), oppfolgingsperiode.getSluttDato(), oppfolgingsperiode.getUuid());
            log.info("lagt til oppfolgingsperiode={} i {} antall aktivitetsversjoner for aktorid={}", oppfolgingsperiode.getUuid(), raderOppdatert, aktorId.get());
        }

        dao.setOppfolgingsperiodeTilUkjentForGamleAktiviteterUtenOppfolgingsperiode(aktorId);

        return true;
    }
}
