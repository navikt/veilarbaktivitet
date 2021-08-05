package no.nav.veilarbaktivitet.nivaa4;

import no.nav.veilarbaktivitet.domain.Person;

import java.util.Optional;

public interface Nivaa4Client {
    Optional<Nivaa4DTO> get(Person.AktorId aktorId);
    void setBaseUrl(String baseUrl);
}
