package no.nav.fo.veilarbaktivitet.ws.consumer;

import no.nav.fo.veilarbaktivitet.domain.Person;
import no.nav.fo.veilarbaktivitet.util.DateUtils;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Deltakerstatuser;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Periode;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Tiltaksaktivitet;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerRequest;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerResponse;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Arrays;
import java.util.Date;

import static no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer.DATOFILTER_PROPERTY_NAME;
import static no.nav.fo.veilarbaktivitet.ws.consumer.ArenaAktivitetConsumer.parseDato;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArenaAktivitetConsumerTest {

    private ApplicationContext setupContext(TiltakOgAktivitetV1 arena) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("TiltakOgAktivitetV1", arena);
        context.register(PropertySourcesPlaceholderConfigurer.class);
        context.register(ArenaAktivitetConsumer.class);
        
        context.refresh();
        context.start();
        return context;
    }


    @Test
    public void springInitialiseringSkalLeseVariabelOgParseDato() {
        System.clearProperty(DATOFILTER_PROPERTY_NAME);
        ApplicationContext context = setupContext(mock(TiltakOgAktivitetV1.class));
        ArenaAktivitetConsumer consumer = context.getBean(ArenaAktivitetConsumer.class);
        assertThat(consumer.arenaAktivitetFilterDato, nullValue());

        System.setProperty(DATOFILTER_PROPERTY_NAME, "2017-08-30");
        context = setupContext(mock(TiltakOgAktivitetV1.class));
        consumer = context.getBean(ArenaAktivitetConsumer.class);
        assertThat(consumer.arenaAktivitetFilterDato, equalTo(parseDato("2017-08-30")));

    }
    
    @Test
    public void skalFiltrereArenaAktiviteterBasertPaaDato() throws Exception {

        TiltakOgAktivitetV1 arena = mock(TiltakOgAktivitetV1.class);
        ArenaAktivitetConsumer consumer = new ArenaAktivitetConsumer(null);
        consumer.tiltakOgAktivitetV1 = arena;
        consumer.arenaAktivitetFilterDato = new Date();

        Date arenaAktivitetFilterDato = consumer.arenaAktivitetFilterDato;

        HentTiltakOgAktiviteterForBrukerResponse responsMedNyAktivitet = 
                responsMedTiltak(new Date(arenaAktivitetFilterDato.getTime() + 1));
        when(arena.hentTiltakOgAktiviteterForBruker(any(HentTiltakOgAktiviteterForBrukerRequest.class)))
                .thenReturn(responsMedNyAktivitet);
        assertThat(consumer.hentArenaAktiviteter(Person.fnr("123")).size(), equalTo(1));

        HentTiltakOgAktiviteterForBrukerResponse responsMedGammelAktivitet = 
                responsMedTiltak(new Date(arenaAktivitetFilterDato.getTime() - 1));
        when(arena.hentTiltakOgAktiviteterForBruker(any(HentTiltakOgAktiviteterForBrukerRequest.class)))
                .thenReturn(responsMedGammelAktivitet);
        assertThat(consumer.hentArenaAktiviteter(Person.fnr("123")).size(), equalTo(0));
    }

    @Test
    public void skalIkkeFiltrereArenaAktiviteterHvisFilterDatoErNull() throws Exception {

        TiltakOgAktivitetV1 arena = mock(TiltakOgAktivitetV1.class);
        ArenaAktivitetConsumer consumer = new ArenaAktivitetConsumer(null);
        consumer.tiltakOgAktivitetV1 = arena;

        HentTiltakOgAktiviteterForBrukerResponse responsMedNyAktivitet = 
                responsMedTiltak(new Date());
        when(arena.hentTiltakOgAktiviteterForBruker(any(HentTiltakOgAktiviteterForBrukerRequest.class)))
                .thenReturn(responsMedNyAktivitet);
        assertThat(consumer.hentArenaAktiviteter(Person.fnr("123")).size(), equalTo(1));
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
