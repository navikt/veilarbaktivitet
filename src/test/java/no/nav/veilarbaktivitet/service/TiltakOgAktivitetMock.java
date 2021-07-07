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


//TODO forbedre deenne
@Component
public class TiltakOgAktivitetMock implements TiltakOgAktivitetV1  {

    private static Tiltaksaktivitet opprettTiltaksaktivitet(String status, String id) {
        Tiltaksaktivitet tiltaksaktivitet = new Tiltaksaktivitet();
        Deltakerstatuser ds = new Deltakerstatuser();
        ds.setValue(status);
        tiltaksaktivitet.setTiltaksnavn("Arbeidsmarkedsopplæring (AMO)");
        tiltaksaktivitet.setAktivitetId(id);
        tiltaksaktivitet.setTiltakLokaltNavn("Arbeidslivskunnskap med praksis og bransjenorsk");
        tiltaksaktivitet.setDeltakerStatus(ds);
        return tiltaksaktivitet;
    }

    public static Tiltaksaktivitet opprettAktivTiltaksaktivitet() {
        return opprettTiltaksaktivitet("GJENN", "12");
    }

    public static Tiltaksaktivitet opprettInaktivTiltaksaktivitet() {
        return opprettTiltaksaktivitet("GJENN_AVB","11");
    }

    @Override
    public HentTiltakOgAktiviteterForBrukerResponse hentTiltakOgAktiviteterForBruker(HentTiltakOgAktiviteterForBrukerRequest hentTiltakOgAktiviteterForBrukerRequest) throws HentTiltakOgAktiviteterForBrukerPersonIkkeFunnet, HentTiltakOgAktiviteterForBrukerSikkerhetsbegrensning, HentTiltakOgAktiviteterForBrukerUgyldigInput {
        HentTiltakOgAktiviteterForBrukerResponseMock tiltakResponseMock = new HentTiltakOgAktiviteterForBrukerResponseMock();
        Tiltaksaktivitet t1 = opprettInaktivTiltaksaktivitet();
        tiltakResponseMock.leggTilTiltak(t1);

        Tiltaksaktivitet t2 = opprettAktivTiltaksaktivitet();
        tiltakResponseMock.leggTilTiltak(t2);

        return tiltakResponseMock;
    }

    @Override
    public void ping() {

    }

}
