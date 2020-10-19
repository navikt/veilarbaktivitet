package no.nav.veilarbaktivitet.service;

import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.HentTiltakOgAktiviteterForBrukerPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.HentTiltakOgAktiviteterForBrukerSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.HentTiltakOgAktiviteterForBrukerUgyldigInput;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Deltakerstatuser;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Tiltaksaktivitet;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerRequest;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerResponse;
import no.nav.veilarbaktivitet.mock.HentTiltakOgAktiviteterForBrukerResponseMock;
import org.springframework.stereotype.Component;

@Component
public class TiltakOgAktivitetMock implements TiltakOgAktivitetV1  {

    @Override
    public HentTiltakOgAktiviteterForBrukerResponse hentTiltakOgAktiviteterForBruker(HentTiltakOgAktiviteterForBrukerRequest hentTiltakOgAktiviteterForBrukerRequest) throws HentTiltakOgAktiviteterForBrukerPersonIkkeFunnet, HentTiltakOgAktiviteterForBrukerSikkerhetsbegrensning, HentTiltakOgAktiviteterForBrukerUgyldigInput {
        HentTiltakOgAktiviteterForBrukerResponseMock tiltak = new HentTiltakOgAktiviteterForBrukerResponseMock();
        Tiltaksaktivitet t1 = new Tiltaksaktivitet();
        Deltakerstatuser ds = new Deltakerstatuser();
        ds.setValue("GJENN_AVB");
        t1.setTiltaksnavn("Arbeidsmarkedsopplæring (AMO)");
        t1.setAktivitetId("11");
        t1.setTiltakLokaltNavn("Arbeidslivskunnskap med praksis og bransjenorsk");
        t1.setDeltakerStatus(ds);
        tiltak.setTiltak(t1);
        return tiltak;
    }

    @Override
    public void ping() {

    }

}
