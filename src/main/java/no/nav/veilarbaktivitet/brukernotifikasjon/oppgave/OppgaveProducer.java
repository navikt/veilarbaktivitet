package no.nav.veilarbaktivitet.brukernotifikasjon.oppgave;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.brukernotifikasjon.schemas.builders.domain.PreferertKanal;
import no.nav.brukernotifikasjon.schemas.builders.legacy.OppgaveBuilder;
import no.nav.common.kafka.producer.KafkaProducerClient;
import no.nav.common.utils.Credentials;
import no.nav.veilarbaktivitet.person.Person;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RequiredArgsConstructor
@Service
public class OppgaveProducer {
    private final KafkaProducerClient<Nokkel, Oppgave> kafkaOppgaveProducer;
    private final Credentials serviceUserCredentials;
    @Value("${topic.ut.brukernotifikasjon.oppgave}")
    private String oppgaveToppic;

    @SneakyThrows
    long sendOppgave(SkalSendes skalSendes, Person.Fnr fnr, URL aktivitetsLink) {
        int sikkerhetsnivaa = 3;

        Nokkel nokkel = new Nokkel(serviceUserCredentials.username, skalSendes.getBrukernotifikasjonId());

        Oppgave oppgave = new OppgaveBuilder()
                .withTidspunkt(LocalDateTime.now(ZoneOffset.UTC))
                .withFodselsnummer(fnr.get())
                .withGrupperingsId(skalSendes.getOppfolgingsperiode())
                .withTekst(skalSendes.getMelding())
                .withLink(aktivitetsLink)
                .withSikkerhetsnivaa(sikkerhetsnivaa)
                .withEksternVarsling(true)
                .withSmsVarslingstekst(skalSendes.getSmsTekst())
                .withPrefererteKanaler(PreferertKanal.SMS)
                .withSmsVarslingstekst(skalSendes.getSmsTekst())
                .withEpostVarslingstittel(skalSendes.getEpostTitel())
                .withEpostVarslingstekst(skalSendes.getEpostBody())
                .build();

        final ProducerRecord<Nokkel, Oppgave> kafkaMelding = new ProducerRecord<>(oppgaveToppic, nokkel, oppgave);
        return kafkaOppgaveProducer.send(kafkaMelding).get().offset();
    }

}
