package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveBuilder;
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.utils.Credentials;
import no.nav.veilarbaktivitet.brukernotifikasjon.VarselStatus;
import no.nav.veilarbaktivitet.domain.Person;
import no.nav.veilarbaktivitet.service.AuthService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
class BrukerNotifkasjonOppgaveService {
    private final Credentials serviceUserCredentials;
    private final OppgaveDao dao;
    private final KafkaProducerClient<Nokkel, Oppgave> producer;
    private final AuthService authService;

    @Value("${topic.ut.brukernotifikasjon.oppgave}")
    private String oppgaveToppic;
    @Value("${app.env.aktivitetsplan.basepath}")
    private String aktivitetsplanBasepath;

    @Transactional
    public void send(SkalSendes skalSendes) {
        if (skalSendes.skalAbrytes()) {
            dao.oppdaterStatus(skalSendes.getId(), VarselStatus.PENDING, VarselStatus.AVBRUTT);
            return;
        }

        boolean oppdatertOk = dao.oppdaterStatus(skalSendes.getId(), VarselStatus.PENDING, VarselStatus.FORSOKT_SENDT);

        if (oppdatertOk) {
            sendOppgave(skalSendes);
        }
    }


    private void sendOppgave(SkalSendes skalSendes) {
        Person.Fnr fnr = authService.getFnrForAktorId(Person.aktorId(skalSendes.getAktorId()));

        Nokkel nokkel = new Nokkel(serviceUserCredentials.username, skalSendes.getBrukernotifikasjonId());
        Oppgave oppgave = createOppgave(skalSendes.getAktivitetId(), fnr, skalSendes.getMelding(), skalSendes.getOppfolgingsperiode());
        final ProducerRecord<Nokkel, Oppgave> kafkaMelding = new ProducerRecord<>(oppgaveToppic, nokkel, oppgave);
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
                .withTidspunkt(LocalDateTime.now())
                .withFodselsnummer(fnr.toString())
                .withGrupperingsId(oppfolgingsPeriode)
                .withTekst(tekst)
                .withLink(link)
                .withSikkerhetsnivaa(sikkerhetsnivaa)
                .withEksternVarsling(true)
                .withPrefererteKanaler(PreferertKanal.SMS)
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

    List<SkalSendes> hentVarselSomSkalSendes(int maxAntall) {
        return dao.hentVarselSomSkalSendes(maxAntall);
    }
}
