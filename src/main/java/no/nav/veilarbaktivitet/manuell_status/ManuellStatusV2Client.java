package no.nav.veilarbaktivitet.manuell_status;

import no.nav.veilarbaktivitet.domain.Person;

import java.util.Optional;

public interface ManuellStatusV2Client {
    Optional<ManuellStatusV2Response> get(Person.AktorId aktorId);

    void setBaseUrl(String baseUrl);
}
