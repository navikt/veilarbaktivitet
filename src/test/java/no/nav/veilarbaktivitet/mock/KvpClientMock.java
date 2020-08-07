package no.nav.veilarbaktivitet.mock;

import no.nav.veilarbaktivitet.client.KvpClient;
import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.domain.Person;

public class KvpClientMock implements KvpClient {
    @Override
    public KvpDTO get(Person.AktorId aktorId) {
        return null;
    }
}
