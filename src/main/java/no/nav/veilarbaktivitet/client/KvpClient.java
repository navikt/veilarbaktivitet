package no.nav.veilarbaktivitet.client;

import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.domain.Person;

import java.util.Optional;

public interface KvpClient {
    Optional<KvpDTO> get(Person.AktorId aktorId);
}
