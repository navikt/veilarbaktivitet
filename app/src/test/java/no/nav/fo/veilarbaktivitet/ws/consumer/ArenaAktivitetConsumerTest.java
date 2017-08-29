package no.nav.fo.veilarbaktivitet.ws.consumer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;

import org.junit.Test;

import no.nav.fo.veilarbaktivitet.util.DateUtils;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Deltakerstatuser;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Periode;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Tiltaksaktivitet;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerRequest;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerResponse;

public class ArenaAktivitetConsumerTest {

    @Test
    public void skalFiltrereArenaAktiviteterBasertPaaDato() throws Exception {
        ArenaAktivitetConsumer consumer = new ArenaAktivitetConsumer();
        TiltakOgAktivitetV1 arena = mock(TiltakOgAktivitetV1.class);
        consumer.tiltakOgAktivitetV1 = arena;

        Date arenaAktivitetFilterDato = consumer.arenaAktivitetFilterDato;       

        HentTiltakOgAktiviteterForBrukerResponse responsMedNyAktivitet = 
                responsMedTiltak(new Date(arenaAktivitetFilterDato.getTime() + 1));
        when(arena.hentTiltakOgAktiviteterForBruker(any(HentTiltakOgAktiviteterForBrukerRequest.class)))
                .thenReturn(responsMedNyAktivitet);
        assertThat(consumer.hentArenaAktivieter("123").size(), is(1));

        HentTiltakOgAktiviteterForBrukerResponse responsMedGammelAktivitet = 
                responsMedTiltak(new Date(arenaAktivitetFilterDato.getTime() - 1));
        when(arena.hentTiltakOgAktiviteterForBruker(any(HentTiltakOgAktiviteterForBrukerRequest.class)))
                .thenReturn(responsMedGammelAktivitet);
        assertThat(consumer.hentArenaAktivieter("123").size(), is(0));
    }

    private HentTiltakOgAktiviteterForBrukerResponse responsMedTiltak(Date date) {
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
}
