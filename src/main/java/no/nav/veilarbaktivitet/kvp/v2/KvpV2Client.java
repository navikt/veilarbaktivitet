package no.nav.veilarbaktivitet.kvp.v2;

import no.nav.veilarbaktivitet.person.Person;

import java.util.Optional;

public interface KvpV2Client {
    Optional<KvpV2DTO> get(Person.AktorId aktorId);

    void setBaseUrl(String baseUrl);
}
