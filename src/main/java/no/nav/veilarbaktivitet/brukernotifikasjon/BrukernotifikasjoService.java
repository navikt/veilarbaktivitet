package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveBuilder;


import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingV2Client;
import no.nav.veilarbaktivitet.service.AuthService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BrukernotifikasjoService {
    private final KafkaTemplate<Nokkel, Oppgave> producerClient;
    @Value("${app.env.brukernotifikasjon.bestillingstoppic}")
    private final String brukernotifikasjontoppic;
    @Value("${app.env.aktivitetsplan.basepath}")
    private final String aktivitetsplanBasepath;
    private final AuthService authService;
    private final OppfolgingV2Client oppfolgingClient;

    public void sendOppgavePaaAktivitet(
            long aktivitetId,
            Person.AktorId aktorId,
            String tekst,
            Varseltype varseltype
    ) {
        Nokkel nokkel = new Nokkel("srvveilarbaktivitet", UUID.randomUUID().toString());
        Person.Fnr fnr = authService
                .getFnrForAktorId(aktorId)
                .orElseThrow(() -> new IllegalArgumentException("ugyldig aktirId"));

        OppfolgingPeriodeMinimalDTO oppfolging = oppfolgingClient.getGjeldendePeriode(aktorId)
                .orElseThrow(() -> new IllegalStateException("bruker ikke under oppfolging"));

        Oppgave oppgave = createOppgave(aktivitetId, fnr, tekst, oppfolging.getUuid().toString());
        final ProducerRecord<Nokkel, Oppgave> record = new ProducerRecord<>(brukernotifikasjontoppic, nokkel, oppgave);
        producerClient.send(record);
    }

    private Oppgave createOppgave(
            long aktivitetId,
            Person.Fnr fnr,
            String tekst,
            String oppfolgingsPeriode
    ) {
        URL link = crateAktivitetLink(aktivitetId);
        int sikkerhetsnivaa = 4; //TODO kan vi bruke nivaa 3?
        return new OppgaveBuilder()
                .withTidspunkt(LocalDateTime.now()) //TODO fiks tidsone finn ut av denene
                .withFodselsnummer(fnr.toString())
                .withGrupperingsId(oppfolgingsPeriode)
                .withTekst(tekst)
                .withLink(link)
                .withSikkerhetsnivaa(sikkerhetsnivaa)
                .withEksternVarsling(true)
                .withPrefererteKanaler(PreferertKanal.SMS, PreferertKanal.EPOST)//TODO skal vi sende epost
                .build();
    }

    private URL crateAktivitetLink(long aktivitetId) {
        URL link = null;
        try {
            link = new URL(aktivitetsplanBasepath + "/aktivitet/vis/" + aktivitetId);
        } catch (MalformedURLException e) {
            log.error("URL hadde ugyldig format", e);
        }
        return link;
    }
}
