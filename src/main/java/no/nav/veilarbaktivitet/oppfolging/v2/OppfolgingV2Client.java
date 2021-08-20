package no.nav.veilarbaktivitet.oppfolging.v2;

import no.nav.veilarbaktivitet.domain.Person;

import java.util.Optional;

public interface OppfolgingV2Client {
    Optional<OppfolgingV2Response> get(Person.AktorId aktorId);

    void setBaseUrl(String baseUrl);
}
