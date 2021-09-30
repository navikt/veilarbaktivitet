package no.nav.veilarbaktivitet.arena;

import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.binding.TiltakOgAktivitetV1;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Deltakerstatuser;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Periode;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.informasjon.Tiltaksaktivitet;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerRequest;
import no.nav.tjeneste.virksomhet.tiltakogaktivitet.v1.meldinger.HentTiltakOgAktiviteterForBrukerResponse;
import no.nav.veilarbaktivitet.person.Person;
import no.nav.veilarbaktivitet.util.DateUtils;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Arrays;
import java.util.Date;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
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
        clearProperty(no.nav.veilarbaktivitet.config.ApplicationContext.ARENA_AKTIVITET_DATOFILTER_PROPERTY);
        ApplicationContext context = setupContext(mock(TiltakOgAktivitetV1.class));
        ArenaAktivitetConsumer consumer = context.getBean(ArenaAktivitetConsumer.class);
        assertThat(consumer.arenaAktivitetFilterDato, nullValue());

        setProperty(no.nav.veilarbaktivitet.config.ApplicationContext.ARENA_AKTIVITET_DATOFILTER_PROPERTY, "2017-08-30");
        context = setupContext(mock(TiltakOgAktivitetV1.class));
        consumer = context.getBean(ArenaAktivitetConsumer.class);
        assertThat(consumer.arenaAktivitetFilterDato, equalTo(ArenaAktivitetConsumer.parseDato("2017-08-30")));

    }

    @Test
    public void skalFiltrereArenaAktiviteterBasertPaaDato() throws Exception {

        TiltakOgAktivitetV1 arena = mock(TiltakOgAktivitetV1.class);
        ArenaAktivitetConsumer consumer = new ArenaAktivitetConsumer(arena);
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
        ArenaAktivitetConsumer consumer = new ArenaAktivitetConsumer(arena);

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

    @Test
    public void skalViseTiltaksvariantPaaGruppeAMO () throws Exception {
        TiltakOgAktivitetV1 arena = mock(TiltakOgAktivitetV1.class);
        ArenaAktivitetConsumer consumer = new ArenaAktivitetConsumer(arena);

        HentTiltakOgAktiviteterForBrukerResponse responsMedNyAktivitet = responsMedTiltak(new Date());
        responsMedNyAktivitet.getTiltaksaktivitetListe().get(0).setTiltaksnavn("Gruppe AMO");

        when(arena.hentTiltakOgAktiviteterForBruker(any(HentTiltakOgAktiviteterForBrukerRequest.class)))
                .thenReturn(responsMedNyAktivitet);

        var aktivitet = consumer.hentArenaAktiviteter(Person.fnr("123")).get(0);
        assertThat(aktivitet.getTittel(), equalTo("Gruppe AMO: " + aktivitet.getTiltakLokaltNavn()));
    }
}
