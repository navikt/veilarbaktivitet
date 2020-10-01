package no.nav.veilarbaktivitet.ws.consumer;

import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Deltakerstatuser;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Periode;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Tiltaksaktivitet;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerRequest;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerResponse;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.junit.Test;
import java.time.ZonedDateTime;
import java.util.Arrays;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArenaAktivitetConsumerTest {

    @Test
    public void skalIkkeFiltrereArenaAktiviteterHvisFilterDatoErNull() throws Exception {

        TiltakOgAktivitetV1 arena = mock(TiltakOgAktivitetV1.class);
        ArenaAktivitetConsumer consumer = new ArenaAktivitetConsumer(arena);

        HentTiltakOgAktiviteterForBrukerResponse responsMedNyAktivitet =
                responsMedTiltak(ZonedDateTime.now());
        when(arena.hentTiltakOgAktiviteterForBruker(any(HentTiltakOgAktiviteterForBrukerRequest.class)))
                .thenReturn(responsMedNyAktivitet);
        assertThat(consumer.hentArenaAktiviteter(Person.fnr("123")).size(), equalTo(1));
    }

    private HentTiltakOgAktiviteterForBrukerResponse responsMedTiltak(ZonedDateTime date) {
        HentTiltakOgAktiviteterForBrukerResponse arenaResponse = mock(HentTiltakOgAktiviteterForBrukerResponse.class);
        Tiltaksaktivitet tiltak = new Tiltaksaktivitet();
        Periode periode = new Periode();
        periode.setTom(DateUtils.xmlCalendar(date));
        tiltak.setDeltakelsePeriode(periode);
        tiltak.setTiltaksnavn("Navn");
        Deltakerstatuser status = new Deltakerstatuser();
        status.setValue("AKTUELL");
        tiltak.setDeltakerStatus(status);
        when(arenaResponse.getTiltaksaktivitetListe()).thenReturn(Arrays.asList(tiltak));
        return arenaResponse;
    }

    @Test
    public void skalViseTiltaksvariantPaaGruppeAMO () throws Exception {
        TiltakOgAktivitetV1 arena = mock(TiltakOgAktivitetV1.class);
        ArenaAktivitetConsumer consumer = new ArenaAktivitetConsumer(arena);

        HentTiltakOgAktiviteterForBrukerResponse responsMedNyAktivitet = responsMedTiltak(ZonedDateTime.now());
        responsMedNyAktivitet.getTiltaksaktivitetListe().get(0).setTiltaksnavn("Gruppe AMO");

        when(arena.hentTiltakOgAktiviteterForBruker(any(HentTiltakOgAktiviteterForBrukerRequest.class)))
                .thenReturn(responsMedNyAktivitet);

        var aktivitet = consumer.hentArenaAktiviteter(Person.fnr("123")).get(0);
        assertThat(aktivitet.getTittel(), equalTo("Gruppe AMO: " + aktivitet.getTiltakLokaltNavn()));
    }
}
