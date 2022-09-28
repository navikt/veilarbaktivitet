package no.nav.veilarbaktivitet.nivaa4;

import no.nav.veilarbaktivitet.person.Person;

import java.util.Optional;

public interface Nivaa4Client {
    Optional<Nivaa4DTO> get(Person.AktorId aktorId);
}
