package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveBuilder;


import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.utils.Credentials;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingPeriodeMinimalDTO;
import no.nav.veilarbaktivitet.oppfolging.v2.OppfolgingV2Client;
import no.nav.veilarbaktivitet.service.AuthService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BrukernotifikasjonService {
    @Value("${topic.ut.brukernotifikasjon.oppgave}")
    private String brukernotifikasjontoppic;
    @Value("${app.env.aktivitetsplan.basepath}")
    private String aktivitetsplanBasepath;
    private final AuthService authService;
    private final OppfolgingV2Client oppfolgingClient;
    private final BrukerNotifikasjonDAO brukerNotifikasjonDAO;
    private final Credentials serviceUserCredentials;
    private final KafkaProducerClient<Nokkel, Oppgave> producer;

    public void opprettOppgavePaaAktivitet(
            long aktivitetId,
            long aktitetVersion,
            Person.AktorId aktorId,
            String tekst,
            Varseltype varseltype
    ) {
        UUID uuid = UUID.randomUUID();

        Person.Fnr fnr = authService
                .getFnrForAktorId(aktorId)
                .orElseThrow(() -> new IllegalArgumentException("ugyldig aktorId"));

        OppfolgingPeriodeMinimalDTO oppfolging = oppfolgingClient.getGjeldendePeriode(aktorId)
                .orElseThrow(() -> new IllegalStateException("bruker ikke under oppfolging"));

        brukerNotifikasjonDAO.opprettBrukernotifikasjon(uuid, aktivitetId, aktitetVersion, fnr, tekst, oppfolging.getUuid(), varseltype, VarselStatus.PENDING);
    }

    //TODO trigger denne.
    private void sendOppgave(long aktivitetId, String tekst, UUID uuid, Person.Fnr fnr, OppfolgingPeriodeMinimalDTO oppfolging) {
        Nokkel nokkel = new Nokkel(serviceUserCredentials.username, uuid.toString());
        Oppgave oppgave = createOppgave(aktivitetId, fnr, tekst, oppfolging.getUuid().toString());
        final ProducerRecord<Nokkel, Oppgave> kafkaMelding = new ProducerRecord<>(brukernotifikasjontoppic, nokkel, oppgave);
        producer.send(kafkaMelding);
    }

    private Oppgave createOppgave(
            long aktivitetId,
            Person.Fnr fnr,
            String tekst,
            String oppfolgingsPeriode
    ) {
        URL link = crateAktivitetLink(aktivitetId);
        int sikkerhetsnivaa = 3;
        return new OppgaveBuilder()
                .withTidspunkt(LocalDateTime.now())//TODO fiks tidsone finn ut av denene
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
