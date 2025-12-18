package no.nav.veilarbaktivitet.kvp.v2;

import no.nav.veilarbaktivitet.person.Person;

import java.util.Optional;

public interface KvpV2Client {
    Optional<KontorSperre> get(Person.Fnr aktorId);

    void setBaseUrl(String baseUrl);
}
