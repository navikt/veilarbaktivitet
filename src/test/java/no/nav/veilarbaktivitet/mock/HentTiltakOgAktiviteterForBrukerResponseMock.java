package no.nav.veilarbaktivitet.mock;

import java.util.*;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Tiltaksaktivitet;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerResponse;

public class HentTiltakOgAktiviteterForBrukerResponseMock
	extends HentTiltakOgAktiviteterForBrukerResponse {

	public void leggTilTiltak(Tiltaksaktivitet tiltak) {
		tiltaksaktivitetListe = new ArrayList<Tiltaksaktivitet>();
		tiltaksaktivitetListe.add(tiltak);
	}
}
