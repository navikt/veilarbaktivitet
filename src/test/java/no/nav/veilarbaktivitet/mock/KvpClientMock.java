package no.nav.veilarbaktivitet.mock;

import java.util.Optional;
import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.kvp.KvpClient;

public class KvpClientMock implements KvpClient {

	@Override
	public Optional<KvpDTO> get(Person.AktorId aktorId) {
		return null;
	}
}
