package no.nav.veilarbaktivitet.oppfolging_status;

import no.nav.veilarbaktivitet.domain.Person;

import java.util.Optional;

public interface OppfolgingStatusClient {
    Optional<OppfolgingStatusDTO> get(Person.AktorId aktorId);

}
