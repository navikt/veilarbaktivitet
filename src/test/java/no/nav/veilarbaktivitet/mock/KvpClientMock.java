package no.nav.veilarbaktivitet.mock;

import no.nav.veilarbaktivitet.kvp.KvpClient;
import no.nav.veilarbaktivitet.kvp.KvpDTO;
import no.nav.veilarbaktivitet.domain.Person;

import java.util.Optional;

public class KvpClientMock implements KvpClient {
    @Override
    public Optional<KvpDTO> get(Person.AktorId aktorId) {
        return null;
    }
}
