package no.nav.veilarbaktivitet.oppfolging.client;

import no.nav.veilarbaktivitet.person.Person;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OppfolgingClient {
    Optional<OppfolgingV2UnderOppfolgingDTO> fetchUnderoppfolging(Person.AktorId aktorId);

    Optional<OppfolgingPeriodeMinimalDTO> fetchGjeldendePeriode(Person.AktorId aktorId);

    List<OppfolgingPeriodeMinimalDTO> hentOppfolgingsperioder(Person.AktorId aktorId);

    Optional<SakDTO> hentSak(UUID oppfolgingsperiodeId);
}
