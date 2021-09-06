package no.nav.veilarbaktivitet.oppfolging.v1;

import no.nav.veilarbaktivitet.domain.Person;

import java.util.Optional;

public interface OppfolgingStatusClient {
    Optional<OppfolgingStatusDTO> get(Person.AktorId aktorId);

    void setBaseUrl(String baseUrl);

}
