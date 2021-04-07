package no.nav.veilarbaktivitet.kvp;

import java.util.Optional;
import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.domain.Person;

public interface KvpClient {
	Optional<KvpDTO> get(Person.AktorId aktorId);
}
