package no.nav.veilarbaktivitet.client;

import no.nav.common.health.HealthCheck;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.domain.arena.ArenaAktivitetDTO;

import java.util.List;

public interface ArenaAktivitetClient extends HealthCheck {

    List<ArenaAktivitetDTO> hentArenaAktiviteter(Person.Fnr personident);

}
