package no.nav.veilarbaktivitet.oppfolging.v2;

import no.nav.veilarbaktivitet.person.Person;

import java.util.List;
import java.util.Optional;

public interface OppfolgingV2Client {
    Optional<OppfolgingV2UnderOppfolgingDTO> fetchUnderoppfolging(Person.AktorId aktorId);

    Optional<OppfolgingPeriodeMinimalDTO> fetchGjeldendePeriode(Person.AktorId aktorId);

    Optional<List<OppfolgingPeriodeMinimalDTO>> hentOppfolgingsPerioder(Person.AktorId aktorId);
}
