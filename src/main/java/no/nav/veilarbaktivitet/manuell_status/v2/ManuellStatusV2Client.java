package no.nav.veilarbaktivitet.manuell_status.v2;

import no.nav.veilarbaktivitet.person.Person;

import java.util.Optional;

public interface ManuellStatusV2Client {
    Optional<ManuellStatusV2DTO> get(Person.AktorId aktorId);

    void setBaseUrl(String baseUrl);
}
