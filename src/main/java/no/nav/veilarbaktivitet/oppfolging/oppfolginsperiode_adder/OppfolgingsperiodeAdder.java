package no.nav.veilarbaktivitet.oppfolging.oppfolginsperiode_adder;

import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingV2Client;
import no.nav.veilarbaktivitet.person.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OppfolgingsperiodeAdder {
    private final AdderDao dao;
    private final OppfolgingV2Client client;

    @Transactional
    public boolean addOppfolingsperioderForEnBruker() {
        Person.AktorId aktorId = dao.hentEnBrukerUtenOppfolgingsperiode();

        if (aktorId == null) {
            return false;
        }

        // TODO vurder eventuell feilhåndtering.
        List<OppfolgingPeriodeMinimalDTO> oppfolgingperioder = client.hentOppfolgingsPerioder(aktorId).get();

        for (OppfolgingPeriodeMinimalDTO oppfolgingsperiode :
                oppfolgingperioder) {
            dao.oppdaterAktiviteterForPeriode(aktorId, oppfolgingsperiode.getStartDato(), oppfolgingsperiode.getSluttDato(), oppfolgingsperiode.getUuid());
        }

        return true;


    }

}
