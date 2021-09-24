package no.nav.veilarbaktivitet.oppfolging.v2;

import no.nav.veilarbaktivitet.person.Person;

import java.util.Optional;

public interface OppfolgingV2Client {
    Optional<OppfolgingV2UnderOppfolgingDTO> getUnderoppfolging(Person.AktorId aktorId);

    Optional<OppfolgingPeriodeMinimalDTO> getGjeldendePeriode(Person.AktorId aktorId);

    void setBaseUrl(String baseUrl);
}
