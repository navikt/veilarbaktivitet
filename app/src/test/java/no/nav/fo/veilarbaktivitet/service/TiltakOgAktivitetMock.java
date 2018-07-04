package no.nav.fo.veilarbaktivitet.service;

import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.HentTiltakOgAktiviteterForBrukerPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.HentTiltakOgAktiviteterForBrukerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.HentTiltakOgAktiviteterForBrukerUgyldigInput;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerRequest;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerResponse;
import org.springframework.stereotype.Component;

@Component
public class TiltakOgAktivitetMock implements TiltakOgAktivitetV1  {

    @Override
    public HentTiltakOgAktiviteterForBrukerResponse hentTiltakOgAktiviteterForBruker(HentTiltakOgAktiviteterForBrukerRequest hentTiltakOgAktiviteterForBrukerRequest) throws HentTiltakOgAktiviteterForBrukerPersonIkkeFunnet, HentTiltakOgAktiviteterForBrukerSikkerhetsbegrensning, HentTiltakOgAktiviteterForBrukerUgyldigInput {
        return null;
    }

    @Override
    public void ping() {

    }

}
