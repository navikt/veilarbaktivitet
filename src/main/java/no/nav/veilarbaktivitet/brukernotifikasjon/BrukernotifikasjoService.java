package no.nav.veilarbaktivitet.brukernotifikasjon;

import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveBuilder;


import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;

@Service
@Slf4j
public class BrukernotifikasjoService {
    private final KafkaTemplate<Nokkel, Oppgave> producerClient;
    private final String brukernotifikasjontoppic;
    private final String aktivitetsplanBasepath;

    public BrukernotifikasjoService(
            KafkaTemplate<Nokkel, Oppgave> producerClient,
            @Value("${app.env.brukernotifikasjon.bestillingstoppic}")
            String oppgaveToppic,
            @Value("${app.env.aktivitetsplan.basepath}")
            String aktivitetsplanBasepath) {
        this.producerClient = producerClient;
        this.brukernotifikasjontoppic = oppgaveToppic;
        this.aktivitetsplanBasepath = aktivitetsplanBasepath;
    }


    public void sendOppgavePaaAktivitet(AktivitesOppgave aktivitesOppgave) {
        Nokkel nokkel = new Nokkel("stvveilarbaktivitet", aktivitesOppgave.getVarselId().toString());
        Oppgave oppgave = createOppgave(aktivitesOppgave);
        final ProducerRecord<Nokkel, Oppgave> record = new ProducerRecord<>(brukernotifikasjontoppic, nokkel, oppgave);
        producerClient.send(record);
    }

    private Oppgave createOppgave(AktivitesOppgave aktivitesOppgave) {
        LocalDateTime now = LocalDateTime.now();
        URL link = crateAktivitetLink(aktivitesOppgave);
        int sikkerhetsnivaa = 4;
        return new OppgaveBuilder()
                .withTidspunkt(now)
                .withFodselsnummer(aktivitesOppgave.getFnr().toString())
                .withGrupperingsId(aktivitesOppgave.getOppfolgingsPeriode())
                .withTekst(aktivitesOppgave.getTekst())
                .withLink(link)
                .withSikkerhetsnivaa(sikkerhetsnivaa)
                .withEksternVarsling(true)
                .withPrefererteKanaler(PreferertKanal.SMS, PreferertKanal.EPOST)//TODO skal vi sende epost
                .build();
    }

    private URL crateAktivitetLink(AktivitesOppgave aktivitesOppgave) {
        URL link = null;
        try {
            link = new URL(aktivitetsplanBasepath  + "/aktivitet/vis/" + aktivitesOppgave.getAktivitetId());
        } catch (MalformedURLException e) {
            log.error("URL hadde ugyldig format", e);
        }
        return link;
    }
}
