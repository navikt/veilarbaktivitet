package no.nav.veilarbaktivitet.client;

import no.nav.veilarbaktivitet.domain.KvpDTO;
import no.nav.veilarbaktivitet.domain.Person;

public interface KvpClient {
    KvpDTO get(Person.AktorId aktorId);
}
