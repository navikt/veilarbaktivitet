package no.nav.veilarbaktivitet.mock;

import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Tiltaksaktivitet;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerResponse;

import java.util.*;

public class HentTiltakOgAktiviteterForBrukerResponseMock extends HentTiltakOgAktiviteterForBrukerResponse {
    public void setTiltak(Tiltaksaktivitet tiltak) {
        tiltaksaktivitetListe = new ArrayList<Tiltaksaktivitet>();
        tiltaksaktivitetListe.add(tiltak);
    }
}
