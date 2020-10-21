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

    private Tiltaksaktivitet opprettTiltaktivitet(String status, String id) {
        Tiltaksaktivitet tiltaksaktivitet = new Tiltaksaktivitet();
        Deltakerstatuser ds = new Deltakerstatuser();
        ds.setValue(status);
        tiltaksaktivitet.setTiltaksnavn("Arbeidsmarkedsopplæring (AMO)");
        tiltaksaktivitet.setAktivitetId(id);
        tiltaksaktivitet.setTiltakLokaltNavn("Arbeidslivskunnskap med praksis og bransjenorsk");
        tiltaksaktivitet.setDeltakerStatus(ds);
        return tiltaksaktivitet;
    }

    @Override
    public HentTiltakOgAktiviteterForBrukerResponse hentTiltakOgAktiviteterForBruker(HentTiltakOgAktiviteterForBrukerRequest hentTiltakOgAktiviteterForBrukerRequest) throws HentTiltakOgAktiviteterForBrukerPersonIkkeFunnet, HentTiltakOgAktiviteterForBrukerSikkerhetsbegrensning, HentTiltakOgAktiviteterForBrukerUgyldigInput {
        HentTiltakOgAktiviteterForBrukerResponseMock tiltakResponseMock = new HentTiltakOgAktiviteterForBrukerResponseMock();
        Tiltaksaktivitet t1 = opprettTiltaktivitet("GJENN_AVB","11");
        tiltakResponseMock.leggTilTiltak(t1);

        Tiltaksaktivitet t2 = opprettTiltaktivitet("GJENN", "12");
        tiltakResponseMock.leggTilTiltak(t2);

        return tiltakResponseMock;
    }

    @Override
    public void ping() {

    }

}
